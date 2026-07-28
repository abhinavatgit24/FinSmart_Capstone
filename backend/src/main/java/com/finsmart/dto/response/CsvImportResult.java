package com.finsmart.dto.response;

import com.finsmart.model.Transaction;
import lombok.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CsvImportResult {
    private int       totalRows;
    private int       imported;
    private int       skipped;
    private List<String>      errors;          // row-level error messages
    private List<Transaction> transactions;    // saved transactions
}
