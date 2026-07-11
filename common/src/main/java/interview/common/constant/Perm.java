package interview.common.constant;

/**
 * @author zhuxi
 */
public interface Perm {

    interface Enterprise {
        String CREATE = "enterprise:create";
        String LIST = "enterprise:list";
        String DETAIL = "enterprise:detail";
        String UPDATE = "enterprise:update";
        String DELETE = "enterprise:delete";
        String UPDATE_CONTACT = "enterprise:update-contact";
    }

    interface Team {
        String LIST = "team:list";
        String INVITE = "team:invite";
        String UPDATE = "team:update";
        String REMOVE = "team:remove";
    }

    interface AdminRoles {
        String LIST = "admin:roles:list";
        String DETAIL = "admin:roles:detail";
    }

    interface AdminUserRoles {
        String ASSIGN = "admin:user-roles:assign";
        String LIST = "admin:user-roles:list";
        String REMOVE = "admin:user-roles:remove";
    }

    interface Job {
        String CREATE = "job:create";
        String LIST = "job:list";
        String DETAIL = "job:detail";
        String UPDATE = "job:update";
        String TOGGLE_STATUS = "job:toggle-status";
        String DELETE = "job:delete";
    }
}
