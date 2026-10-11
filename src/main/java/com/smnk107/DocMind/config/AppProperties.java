package com.smnk107.DocMind.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "app")
@Getter
@Setter
public class AppProperties {

    private CorsProperties cors = new CorsProperties();
    private RagProperties rag = new RagProperties();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CorsProperties {

        private String allowedOrigins = "*";

        private String allowedMethods =
                "GET,POST,PUT,DELETE,OPTIONS";

        private String allowedHeaders = "*";
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RagProperties {

        private int chunkSize = 800;

        private int minChunkSizeChars = 350;

        private int minChunkLengthToEmbed = 5;

        private int maxNumChunks = 10000;

        private boolean keepSeparator = true;

        private String encodingType = "CL100K_BASE";

        private String punctuationMarks = ".?!\n;:";



        private int topK = 5;

        private double similarityThreshold = 0.70;

        public List<Character> getPunctuationList() {
            return punctuationMarks.chars()
                    .mapToObj(c -> (char) c)
                    .toList();
        }
    }
}