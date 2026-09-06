package com.marketpulse.backend.market;

import java.io.IOException;
import java.nio.file.Path;

public interface NewsProvider {
    NewsDataBatch load(Path location) throws IOException;
}