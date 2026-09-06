package com.marketpulse.backend.market;

import java.io.IOException;
import java.nio.file.Path;

public interface MarketDataProvider {
    MarketDataBatch load(Path location) throws IOException;
}