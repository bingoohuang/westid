# westid
Distributed ID generator based on Twitter Snowflake.
<br/>
[![Build Status](https://travis-ci.org/bingoohuang/westid.svg?branch=master)](https://travis-ci.org/bingoohuang/westid)
[![Coverage Status](https://coveralls.io/repos/github/bingoohuang/westid/badge.svg?branch=master)](https://coveralls.io/github/bingoohuang/westid?branch=master)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/westid/badge.svg?style=flat-square)](https://maven-badges.herokuapp.com/maven-central/com.github.bingoohuang/westid/)
[![License](http://img.shields.io/:license-apache-brightgreen.svg)](http://www.apache.org/licenses/LICENSE-2.0.html)


# Usage
Very simple api:
```java
long id = WestId.next();
```

# Structure
a generated id like `1415384785396004` has a structure as following: <br/>
`(timestamp << 22) | (machine << 12) | sequence`

1. 41bits Timestamp(millisecond precision, bespoke epoch)
2. 10bits Configured machine ID(WorkerId)
3. 12bits Sequence number

序列编号有 12 位，意味着每个节点在每毫秒可以产生 4096 个 ID。
中间10位是节点号，比如节点号分成两部分，数据中心的 ID 和节点 ID，各自占 5 位。最简单的实现就是基于IP的后10位。
时间戳则是记录了从 1490346283706L (2017-03-24T17:04:43.706+08:00) 这一时刻到当前时间所经过的毫秒数，占 41 位（还有一位是符号位，永远为 0）。

# Performance
About 4,032,258 ids/second in my macbook pro(2.9 GHz Intel Core i7).

# Worker id assigner bind sequence
## 1) System Property worker id assigner
The worker id can be assigned by system property `westid.workerId`

## 2) System env variable worker id assigner
The worker id can be assigned by system env `WESTID_WORKERID`

## 3) Custom worker id assigner
The user should define class `com.github.bingoohuang.westid.StaticWorkerIdBinder`
which implements `com.github.bingoohuang.westid.WorkerIdAssigner` to assign a worker id.
For example:
```java
package com.github.bingoohuang.westid;

import com.github.bingoohuang.westid.workerid.RedisWorkerIdAssigner;
import redis.clients.jedis.Jedis;

public class StaticWorkerIdBinder implements WorkerIdAssigner {
    @Override
    public int assignWorkerId(WestIdConfig idConfig) {
        return new RedisWorkerIdAssigner(new Jedis()).assignWorkerId(idConfig);
    }
}

```

## 4) IP based worker id assigner
If the all above worker id assigners are not defined, 
the default IP based worker id assigner will be used instead.
It will use the last 10 bits of IP bytes as the worker id.


# Predefined worker id assigner
## Redis based worker id assigner 
class: `com.github.bingoohuang.westid.workerid.RedisWorkerIdAssigner`
<br>从Redis服务器中获取可用的workerId。流程：

1. 尝试重用为当前IP分配过的workerId
    1. 读取workerId:pid:{ip}=[{pid}x{workerId},{pid}x{workerId}]
    2. 加锁（workerId:lok:{ip}={pid}）操作，逐项检查pid的进程是否存在，不存在则可能可以重用
2. 不存在，则从workerId:pid:{ip}删除，并且删除对应的workerId:use:{workerId}（保留最后一个）
3. 有可重用时，返回最后一个workerId，并改写workerId:use:{workerId}={pid}x{ip}x{hostname}
4. 无可重用时（包括加锁失败），查找新的可用workerId
    1. 遍历workerId:use:{0~maxWorkerId}，寻找可以写入的key
    2. 找到时，返回workerId并写入redis list workerId:pid:{ip}=[{pid}x{workerId},{pid}x{workerId}]

## Database based worker id assigner
class: `com.github.bingoohuang.westid.workerid.DbWorkerIdAssigner`
<br>从数据库表中获取可用的workerId

### Create table SQL
```sql
-- MySQL
CREATE TABLE workerid_assign(
    worker_id INT PRIMARY KEY, 
    pid INT NOT NULL, 
    ip VARCHAR(15) NOT NULL, 
    hostname VARCHAR(60) NOT NULL, 
    create_time TIMESTAMP NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- ORACLE
CREATE TABLE workerid_assign(
    worker_id INT PRIMARY KEY, 
    pid INT NOT NULL, 
    ip VARCHAR2(15) NOT NULL, 
    hostname VARCHAR2(60) NOT NULL, 
    create_time TIMESTAMP NOT NULL
);

```

### Process
1. 根据IP查找所有的记录，查看是否有可以重用的workerId（进程号不存在）
2. 有可以重用的workerId时，尝试更新该条记录的（pid,create_time），更新成功则使用当前workerId.
3. 没有可以重用的workerId时，则按顺序查找没有使用的workerId，并且尝试插入表格，插入成功则返回当前的workerId.

### Sample data

| worker_id | pid  |    ip       | hostname |  create_time      |
|    ----   | ---  |    --       | -------- |  -----------      |
|      0    | 1023 | 192.168.1.1 | app-01   |2017-03-28 14:25:25|
|      1    | 3323 | 192.168.1.2 | app-02   |2017-03-28 14:25:28|

# Rantionale
## ID要求
1. Under 64 bits （64比特以下）
2. No co-ordination needed （非协作的）
3. Deterministically unique （确定唯一）
4. k-sorted (time-ordered lexically)

ID 生成要以一种非协作的（uncoordinated）的方式进行，例如不能有一个全局的原子变量。
ID 满足 k-sorted 条件。如果序列 A 要满足 k-sorted，当且仅当对于任意的 p, q，
如果 1 <= p <= q - k (1 <= p <= q <= n)，则有 A[p] <= A[q]。
换句话说，如果元素 p 排在 q 前面，且相差至少 k 个位置，那么 p 必然小于或等于 q。
如果 tweet 序列满足这个条件，要获取第 r 条 tweet 之后的消息，
只要从第 r - k 条开始查找即可。

## 一些认识
1. 41位时间戳，可以用2^41=2199023255552毫秒=69年，69年以后，AI说不定已经占领了地球。
2. 10位任务号，最多1024个任务。
3. 12位序列号，每毫秒最多4096个，每新毫秒重新开始。

最大值[2^63-1=9223372036854775807](http://www.wolframalpha.com/input/?i=2%5E63-1)，19字；
<br>HEX=`7FFFFFFFFFFFFF`，14字；
<br>BASE36=`1y2p0ij32e8e7`，13字；
<br>BASE64URL=`f_________8=`，共12字; 
<br>BASE62=`aZl8N0y58M7`，共11字。

# Extension
## 32位正整形ID生成
1. 21位由毫秒数产生的随机数, 最大值为2097152, 约为34.95分钟;
2. 5位work id, 最大值为32;
3. 5位自增序列, 即每毫秒可产生32个随机数.

微信公众平台需要生成带参数的临时二维码, 而微信API限制二维码参数值为32位非0整型，
配合微信临时二维码的过期时间设置, 若使二维码在生成的随机参数发生碰撞前(即34.95分钟以内)失效, 
则可以在保证参数随机的同时生成唯一的临时二维码

```java
int qrSceneId = QrSceneId.next();
```


# Sonarqube integrated
```fish
set -x http_proxy http://127.0.0.1:9999
set -x https_proxy http://127.0.0.1:9999
sudo gem install travis
travis encrypt encrypt_token

```
