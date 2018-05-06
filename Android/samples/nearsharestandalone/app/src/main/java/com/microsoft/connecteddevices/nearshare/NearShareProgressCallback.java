//
//  Copyright (c) Microsoft Corporation. All rights reserved.
//

package com.microsoft.connecteddevices.nearshare;

import java.util.logging.Level;
import java.util.logging.Logger;

public class NearShareProgressCallback extends ProgressCallback {
    private final static Logger LOG = Logger.getLogger(NearShareProgressCallback.class.getSimpleName());

    @Override
    public void onProgress(NearShareProgress progress) {
        super.onProgress(progress);
        LOG.log(Level.INFO,
            String.format("Progress callback called with: TotalBytesToSend: %1$d, BytesSent: %2$d, FilesToSend: %3$d, FilesSent: %4$d",
                progress.totalBytesToSend, progress.bytesSent, progress.totalFilesToSend, progress.filesSent));
    }
}
