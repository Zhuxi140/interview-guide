package interview.resume.support;

/**
 * 简历集合变更使用的 PostgreSQL Advisory Lock 双键。
 */
public final class ResumeLockKey {

    public static final int NAMESPACE = 0x52534D45;

    private ResumeLockKey() {
    }

    public static int ownerSlot(long userId) {
        return Long.hashCode(userId);
    }
}
