package com.finsmart.service;

import com.finsmart.dto.response.CsvImportResult;
import com.finsmart.model.Transaction;
import com.finsmart.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CsvImportServiceTest {

    @Mock  TransactionRepository transactionRepository;
    @Mock  TransactionService    transactionService;
    @InjectMocks CsvImportService service;

    @BeforeEach
    void setup() {
        when(transactionService.autoCategory(any())).thenReturn("Other");
        when(transactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void importsCsvWithStandardColumns() throws Exception {
        String csv = "date,amount,type,description\n" +
                     "01/05/2025,1000,expense,Swiggy\n" +
                     "02/05/2025,50000,income,Salary\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = service.importCsv("user@test.com", file);

        assertThat(result.getImported()).isEqualTo(2);
        assertThat(result.getSkipped()).isEqualTo(0);
        assertThat(result.getTotalRows()).isEqualTo(2);
    }

    @Test
    void handlesBankStyleDebitCreditColumns() throws Exception {
        String csv = "date,debit,credit,description\n" +
                     "01/05/2025,1500,,Netflix\n" +
                     "02/05/2025,,60000,Salary credit\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "bank.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = service.importCsv("user@test.com", file);

        assertThat(result.getImported()).isEqualTo(2);
        verify(transactionRepository, times(2)).save(any(Transaction.class));
    }

    @Test
    void skipsRowsWithBadDate() throws Exception {
        String csv = "date,amount,type,description\n" +
                     "not-a-date,1000,expense,Bad row\n" +
                     "01/05/2025,500,expense,Good row\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = service.importCsv("user@test.com", file);

        assertThat(result.getImported()).isEqualTo(1);
        assertThat(result.getSkipped()).isEqualTo(1);
        assertThat(result.getErrors()).hasSize(1);
    }

    @Test
    void stripsRupeeSymbolFromAmount() throws Exception {
        String csv = "date,amount,type,description\n" +
                     "01/05/2025,\"₹1,500\",expense,Grocery\n";

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.csv", "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));

        CsvImportResult result = service.importCsv("user@test.com", file);

        assertThat(result.getImported()).isEqualTo(1);
        ArgumentCaptor<Transaction> cap = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(cap.capture());
        assertThat(cap.getValue().getAmount()).isEqualTo(1500.0);
    }

    @Test
    void throwsOnEmptyFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.csv", "text/csv", new byte[0]);

        assertThatThrownBy(() -> service.importCsv("user@test.com", file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("empty");
    }
}
