package com.cesec.springboot.common.configure;

import com.cesec.springboot.common.utils.DircConvertUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
/**
 * ClassName: MetaObjectConfig
 * Description: 字典翻译工具类配置文件
 * Author : zzq
 **/

@Configuration
public class MetaObjectConfig {
    @Bean
    public  DircConvertUtils initDicConvert(){ return new DircConvertUtils();}
    @Bean
    public static ResultSetConvert resultSetConvert(){
        return new ResultSetConvert();
    }
}
