package com.engcoach.parts;

import com.engcoach.attempts.Answer;
import com.engcoach.tests.QuestionGroup;
import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Hibernate 6 trên MySQL ánh xạ @Lob String có length mặc định 255 thành TINYTEXT, làm các bài đọc/nghe dài bị lỗi
 * "Data too long". Test này giữ cho mọi cột @Lob đều khai báo length đủ lớn.
 */
class LongTextColumnsTest {

    @Test
    void everyLobStringColumnDeclaresALengthLargeEnoughForPassagesAndScripts() {
        for (Class<?> type : List.of(Question.class, QuestionGroup.class, Answer.class)) {
            for (Field field : type.getDeclaredFields()) {
                if (field.isAnnotationPresent(Lob.class) && field.getType() == String.class) {
                    Column column = field.getAnnotation(Column.class);
                    assertTrue(column != null && column.length() >= 65535,
                            type.getSimpleName() + "." + field.getName() + " cần @Column(length = ...) lớn hơn 255");
                }
            }
        }
    }
}
