package org.cdsframework.ice.config;

import java.io.IOException;
import java.nio.file.Path;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Configuration
public class PathConfig
{
    private final ResourceLoader resourceLoader;

    @Bean
    public Path configPath() throws IOException
    {
        return Path.of(resourceLoader.getResource("classpath:config").getURI());
    }

    // droolsPath bean removed - rules now loaded from pre-compiled KJAR
}
