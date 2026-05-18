package com.encryprangedb.config;

import com.encryprangedb.ope.ProgrammableOPE;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
public class CryptoBeans {

    @Bean
    public ProgrammableOPE programmableOPE(CryptoProperties props) {
        byte[] key = props.getCrypto().getOpeMasterKey().getBytes(StandardCharsets.UTF_8);
        return new ProgrammableOPE(key, props.getCrypto().getSensitivity());
    }
}

