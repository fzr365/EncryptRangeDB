package com.encryprangedb.service;

import com.encryprangedb.model.PlainInsertRequest;
import com.encryprangedb.model.SqlImportResult;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SqlImportServiceTest {

    @Test
    void importSqlShouldHandleMultiRowValuesInsert() throws Exception {
        RecordService recordService = Mockito.mock(RecordService.class);
        SqlImportService service = new SqlImportService(recordService);

        String sql = """
                INSERT INTO employees (record_id, name, salary) VALUES
                ('emp-1001', 'Alice', 8000),
                ('emp-1002', 'Bob', 12000);
                """;

        SqlImportResult result = service.importSql(sql);

        ArgumentCaptor<PlainInsertRequest> captor = ArgumentCaptor.forClass(PlainInsertRequest.class);
        Mockito.verify(recordService, Mockito.times(2)).insertPlain(captor.capture());

        List<PlainInsertRequest> requests = captor.getAllValues();
        assertEquals(1, result.totalStatements());
        assertEquals(1, result.handledInsertStatements());
        assertEquals(2, result.insertedRows());
        assertTrue(result.errors().isEmpty());

        assertEquals("employees", requests.get(0).table());
        assertEquals("emp-1001", requests.get(0).recordId());
        assertEquals("Alice", requests.get(0).fields().get(0).value());
        assertEquals(8000L, requests.get(0).fields().get(1).value());
        assertTrue(requests.get(0).fields().get(1).indexed());

        assertEquals("emp-1002", requests.get(1).recordId());
        assertEquals("Bob", requests.get(1).fields().get(0).value());
        assertEquals(12000L, requests.get(1).fields().get(1).value());
        assertTrue(requests.get(1).fields().get(1).indexed());
    }
}
