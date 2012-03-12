package de.doridian.ccftp;

import org.apache.ftpserver.ftplet.AuthorizationRequest;

public class MaxFilesizeRequest implements AuthorizationRequest {
    public long max_filesize = 0;

    public long getMaxFilesize() {
        return max_filesize;
    }
    public void setMaxFilesize(long newSize) {
        max_filesize = newSize;
    }
}
