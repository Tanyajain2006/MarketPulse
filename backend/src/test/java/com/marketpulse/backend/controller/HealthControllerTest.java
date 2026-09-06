package com.marketpulse.backend.controller;

import java.sql.Connection;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {
    @Test
    void reportsDatabaseOperationalWhenConnectionIsAvailable() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(connection.isValid(2)).thenReturn(true);
        when(dataSource.getConnection()).thenReturn(connection);

        HealthController.HealthResponse response = new HealthController("marketpulse", dataSource).health();

        assertThat(response.status()).isEqualTo("UP");
        assertThat(response.database()).isEqualTo("UP");
    }

    @Test
    void reportsDegradedWhenDatabaseConnectionFails() throws Exception {
        DataSource dataSource = mock(DataSource.class);
        when(dataSource.getConnection()).thenThrow(new RuntimeException("database unavailable"));

        HealthController.HealthResponse response = new HealthController("marketpulse", dataSource).health();

        assertThat(response.status()).isEqualTo("DEGRADED");
        assertThat(response.database()).isEqualTo("DOWN");
    }
}