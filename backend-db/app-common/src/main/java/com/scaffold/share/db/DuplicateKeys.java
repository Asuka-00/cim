package com.scaffold.share.db;

import java.sql.SQLException;
import java.sql.SQLIntegrityConstraintViolationException;
import java.util.regex.Pattern;

import org.springframework.dao.DuplicateKeyException;

/**
 * DuplicateKeys 唯一约束冲突识别
 * <p>
 * 区分「主键冲突」（同一条记录被重复写入，如 Kafka 至少一次投递重放）与「业务唯一键冲突」
 * （真实数据冲突，不能吞掉）。按各库默认约束命名识别：
 * MySQL {@code for key 'PRIMARY'} / {@code 't.PRIMARY'}；PostgreSQL {@code xxx_pkey}；
 * SQL Server {@code PRIMARY KEY constraint}；Oracle {@code PK_xxx} / {@code xxx_PK}。
 * 无法识别时返回 false（按真实冲突处理）。
 *
 * @author Kairo
 * @version 1.0.0
 * @since 2026-10-09
 */
public final class DuplicateKeys {

    private static final Pattern PRIMARY_KEY = Pattern.compile(
            "for key '([\\w$]+\\.)?PRIMARY'"
                    + "|_pkey\""
                    + "|PRIMARY KEY constraint"
                    + "|unique constraint \\([\\w$]+\\.(PK_[\\w$]*|[\\w$]*_PK)\\)",
            Pattern.CASE_INSENSITIVE);

    private static final int MAX_CAUSE_DEPTH = 10;

    private DuplicateKeys() {
    }

    /**
     * 异常链中是否为主键冲突。
     */
    public static boolean isPrimaryKeyViolation(Throwable ex) {
        Throwable current = ex;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (isDuplicateKeyType(current) && matchesPrimary(current.getMessage())) {
                return true;
            }
            if (current.getCause() == current) {
                break;
            }
            current = current.getCause();
        }
        return false;
    }

    private static boolean isDuplicateKeyType(Throwable ex) {
        if (ex instanceof DuplicateKeyException || ex instanceof SQLIntegrityConstraintViolationException) {
            return true;
        }
        return ex instanceof SQLException sql && sql.getSQLState() != null && sql.getSQLState().startsWith("23");
    }

    private static boolean matchesPrimary(String message) {
        return message != null && PRIMARY_KEY.matcher(message).find();
    }
}
