package com.capstone.jfc.dto.event.payload;

import com.capstone.jfc.enums.ToolTypes;

public final class ParseRequestEventPayload {
    private ToolTypes tool;
    private String tenantId;
    private String filePath;

    public ParseRequestEventPayload() {}

    public ParseRequestEventPayload(ToolTypes tool, String tenantId, String filePath) {
        this.tool = tool;
        this.tenantId = tenantId;
        this.filePath = filePath;
    }

    public ToolTypes getTool() {
        return tool;
    }
    public String getTenantId() {
        return tenantId;
    }
    public String getFilePath() {
        return filePath;
    }
}
