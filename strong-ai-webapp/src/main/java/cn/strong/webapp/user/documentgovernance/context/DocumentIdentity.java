package cn.strong.webapp.user.documentgovernance.context;

import cn.strong.common.exception.ParamValiateException;

public final class DocumentIdentity {

    private final String userId;
    private final String teamId;
    private final String role;

    public DocumentIdentity(String userId, String teamId, String role) {
        this.userId = requireText(userId, "网关用户标识不能为空");
        this.teamId = requireText(teamId, "网关团队标识不能为空");
        this.role = requireText(role, "网关角色不能为空");
    }

    public String getUserId() {
        return userId;
    }

    public String getTeamId() {
        return teamId;
    }

    public String getRole() {
        return role;
    }

    public boolean isAdmin() {
        return "admin".equals(role);
    }

    private String requireText(String value, String message) {
        if (value == null || value.trim().isEmpty()) {
            throw new ParamValiateException(message);
        }
        return value.trim();
    }
}
