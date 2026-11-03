package com.digixmed.cloud.icu.handler;

import com.digixmed.cloud.icu.model.PatientIdentityMapper;
import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * HeightWeightHandler 单元测试：评估单字段合并提取
 */
class HeightWeightHandlerTest {

    private HeightWeightHandler handler;

    @BeforeEach
    void setUp() {
        handler = new HeightWeightHandler(new PatientIdentityMapper());
    }

    /**
     * 构造评估单 Document，fieldValues 为 field/value 成对参数
     */
    private Document form(String formCode, String... fieldValues) {
        Document form = new Document("formCode", formCode);
        List<Document> fieldDataList = new ArrayList<>();
        for (int i = 0; i + 1 < fieldValues.length; i += 2) {
            fieldDataList.add(new Document("field", fieldValues[i]).append("value", fieldValues[i + 1]));
        }
        form.append("fieldDataList", fieldDataList);
        return form;
    }

    @Test
    void testExtractFromSingleForm() {
        List<Document> forms = Collections.singletonList(
                form("zhuanruhulipinggudan", "sg", "175", "tz", "65"));
        assertEquals("175", handler.extractFieldFromForms(forms, Arrays.asList("sg", "fg")));
        assertEquals("65", handler.extractFieldFromForms(forms, Arrays.asList("tz", "zt")));
    }

    @Test
    void testExtractMergesAcrossForms() {
        // 身高在转入单、体重在入院单 -> 合并提取都应命中
        List<Document> forms = Arrays.asList(
                form("ruyuanhulipinggudan", "tz", "65"),
                form("zhuanruhulipinggudan", "sg", "175"));
        assertEquals("175", handler.extractFieldFromForms(forms, Arrays.asList("sg", "fg")));
        assertEquals("65", handler.extractFieldFromForms(forms, Arrays.asList("tz", "zt")));
    }

    @Test
    void testFieldPriorityWinsOverFormOrder() {
        // sg 优先级高于 fg：即使 fg 在新表单中，也取旧表单的 sg
        List<Document> forms = Arrays.asList(
                form("zhuanruhulipinggudan", "fg", "170"),
                form("ruyuanhulipinggudan", "sg", "175"));
        assertEquals("175", handler.extractFieldFromForms(forms, Arrays.asList("sg", "fg")));
    }

    @Test
    void testFallbackFieldUsedWhenPrimaryMissing() {
        List<Document> forms = Collections.singletonList(
                form("ruyuanhulipinggudan", "fg", "170", "zt", "60"));
        assertEquals("170", handler.extractFieldFromForms(forms, Arrays.asList("sg", "fg")));
        assertEquals("60", handler.extractFieldFromForms(forms, Arrays.asList("tz", "zt")));
    }

    @Test
    void testEmptyValueSkipped() {
        // 新表单字段值为空白 -> 取旧表单的值
        List<Document> forms = Arrays.asList(
                form("zhuanruhulipinggudan", "sg", " "),
                form("ruyuanhulipinggudan", "sg", "175"));
        assertEquals("175", handler.extractFieldFromForms(forms, Arrays.asList("sg")));
    }

    @Test
    void testNoFormsOrNoFieldsReturnsNull() {
        assertNull(handler.extractFieldFromForms(null, Arrays.asList("sg")));
        assertNull(handler.extractFieldFromForms(Collections.emptyList(), Arrays.asList("sg")));
        assertNull(handler.extractFieldFromForms(
                Collections.singletonList(form("ruyuanhulipinggudan")), Arrays.asList("sg", "fg")));
    }
}
