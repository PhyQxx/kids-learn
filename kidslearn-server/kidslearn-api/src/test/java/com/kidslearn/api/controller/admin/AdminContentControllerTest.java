package com.kidslearn.api.controller.admin;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Transactional;

class AdminContentControllerTest {

    @Test
    void questionSaveIsTransactional() throws NoSuchMethodException {
        Method method = AdminContentController.class.getMethod("questionSave", java.util.Map.class);

        assertNotNull(method.getAnnotation(Transactional.class));
    }
}
