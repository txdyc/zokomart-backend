package com.zokomart.backend.admin.merchant;

import com.zokomart.backend.admin.merchant.dto.AdminMerchantOrderListResponse;
import com.zokomart.backend.catalog.entity.MerchantEntity;
import com.zokomart.backend.common.exception.BusinessException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class AdminMerchantOrderExportService {

    private static final DateTimeFormatter FILE_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final AdminMerchantService adminMerchantService;

    public AdminMerchantOrderExportService(AdminMerchantService adminMerchantService) {
        this.adminMerchantService = adminMerchantService;
    }

    public ResponseEntity<byte[]> export(
            String merchantId,
            String status,
            String paymentIntentStatus,
            String from,
            String to
    ) {
        MerchantEntity merchant = adminMerchantService.loadMerchantForExport(merchantId);
        List<AdminMerchantOrderListResponse.Item> rows = adminMerchantService
                .listMerchantOrders(merchantId, status, paymentIntentStatus, from, to, 1, Integer.MAX_VALUE)
                .items();

        if (rows.isEmpty()) {
            throw new BusinessException("MERCHANT_ORDER_EXPORT_EMPTY", "当前筛选条件下无可导出订单", HttpStatus.BAD_REQUEST);
        }

        StringBuilder csv = new StringBuilder();
        csv.append("merchantCode,merchantName,orderId,orderNumber,createdAt,buyerId,orderStatus,paymentStatus,paymentIntentExpiresAt,totalAmount,currencyCode");
        for (AdminMerchantOrderListResponse.Item row : rows) {
            csv.append(System.lineSeparator())
                    .append(csvCell(merchant.getMerchantCode())).append(',')
                    .append(csvCell(merchant.getDisplayName())).append(',')
                    .append(csvCell(row.id())).append(',')
                    .append(csvCell(row.orderNumber())).append(',')
                    .append(csvCell(row.createdAt())).append(',')
                    .append(csvCell(row.buyerId())).append(',')
                    .append(csvCell(row.status())).append(',')
                    .append(csvCell(row.paymentIntent() == null ? null : row.paymentIntent().status())).append(',')
                    .append(csvCell(row.paymentIntent() == null ? null : row.paymentIntent().expiresAt())).append(',')
                    .append(csvCell(row.totalAmount())).append(',')
                    .append(csvCell(row.currencyCode()));
        }

        String filename = "merchant-" + merchant.getMerchantCode() + "-orders-" + FILE_TIME_FORMAT.format(LocalDateTime.now()) + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv.toString().getBytes(StandardCharsets.UTF_8));
    }

    private String csvCell(String value) {
        String safeValue = value == null ? "" : value;
        return "\"" + safeValue.replace("\"", "\"\"") + "\"";
    }
}
