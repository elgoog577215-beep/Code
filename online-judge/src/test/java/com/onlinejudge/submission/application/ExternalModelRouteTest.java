package com.onlinejudge.submission.application;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExternalModelRouteTest {

    @Test
    void parseRoutesSkipsMalformedAndIncompleteEntries() {
        assertThat(ExternalModelRoute.parseRoutes("""
                broken;
                ProviderA||key-a|model-a;
                ProviderB|https://b.example/v1|key-b|model-b;
                |https://c.example/v1|key-c|model-c
                """))
                .extracting(ExternalModelRoute::model)
                .containsExactly("model-b", "model-c");
    }

    @Test
    void emptyRoutePoolReturnsNoRoutes() {
        assertThat(ExternalModelRoute.parseRoutes("")).isEmpty();
        assertThat(ExternalModelRoute.parseRoutes(null)).isEmpty();
    }
}
