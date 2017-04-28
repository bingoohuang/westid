package com.github.bingoohuang.westid.extension;

import com.github.bingoohuang.westid.WestId;
import com.github.bingoohuang.westid.WestIdConfig;
import com.github.bingoohuang.westid.WestIdGenerator;
import lombok.experimental.UtilityClass;

/**
 * 32位正整形生成
 * 1. 21位由毫秒数产生的随机数, 最大值为2097152, 约为34.95分钟;
 * 2. 5位work westid, 最大值为32;
 * 3. 5位自增序列, 即每毫秒可产生32个随机数.
 * scene_id	场景值ID，临时二维码时为32位非0整型。
 * http://mp.weixin.qq.com/wiki/18/28fc21e7ed87bec960651f0ce873ef8a.html
 * 微信公众平台需要生成带参数的临时二维码, 而微信API限制二维码参数值为32位非0整型，
 * 配合微信临时二维码的过期时间设置, 若使二维码在生成的随机参数发生碰撞前(即34.95分钟以内)失效,
 * 则可以在保证参数随机的同时生成唯一的临时二维码。
 */
@UtilityClass
public class QrSceneId {
    private final WestIdConfig ID_CONFIG = new WestIdConfig(WestId.EPOCH, 5, 5);
    private final WestIdGenerator ID_GENERATOR = new WestIdGenerator(ID_CONFIG, WestId.bindWorkerId(ID_CONFIG));

    public int next() {
        int next = (int) ID_GENERATOR.next();
        return (next << 1) >>> 1;
    }
}
