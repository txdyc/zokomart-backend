package com.zokomart.backend.merchantadmin.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zokomart.backend.admin.common.AdminAccessPolicy;
import com.zokomart.backend.admin.common.AdminSessionActor;
import com.zokomart.backend.admin.product.dto.AdminProductDetailResponse;
import com.zokomart.backend.admin.product.dto.AdminProductListResponse;
import com.zokomart.backend.common.exception.BusinessException;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductActivateRequest;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductDetailResponse;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductDeactivateRequest;
import com.zokomart.backend.merchantadmin.product.dto.MerchantAdminProductUpsertRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Validated
@RestController
@RequestMapping("/merchant-admin/products")
public class MerchantAdminProductController {

    private final AdminAccessPolicy adminAccessPolicy;
    private final MerchantAdminProductService merchantAdminProductService;
    private final ObjectMapper objectMapper;

    public MerchantAdminProductController(
            AdminAccessPolicy adminAccessPolicy,
            MerchantAdminProductService merchantAdminProductService,
            ObjectMapper objectMapper
    ) {
        this.adminAccessPolicy = adminAccessPolicy;
        this.merchantAdminProductService = merchantAdminProductService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public AdminProductListResponse listProducts(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String merchantId,
            @RequestParam(required = false) String categoryId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        String effectiveMerchantId = resolveMerchantId(actor, merchantId);
        return merchantAdminProductService.listProducts(keyword, status, effectiveMerchantId, categoryId, page, pageSize);
    }

    @GetMapping("/{productId}")
    public MerchantAdminProductDetailResponse getProduct(@PathVariable String productId) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        return merchantAdminProductService.getMerchantProductDetail(actor, productId);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MerchantAdminProductDetailResponse> createProduct(
            @RequestParam("payload") String payload,
            @RequestParam(value = "newImageClientIds", required = false) List<String> newImageClientIds,
            @RequestParam(value = "imageOrder", required = false) List<String> imageOrder,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages
    ) throws JsonProcessingException {
        MerchantAdminProductUpsertRequest request = objectMapper.readValue(payload, MerchantAdminProductUpsertRequest.class);
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        MerchantAdminProductDetailResponse response = merchantAdminProductService.createProduct(actor, request, newImageClientIds, newImages, imageOrder);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MerchantAdminProductDetailResponse updateProduct(
            @PathVariable String productId,
            @RequestParam("payload") String payload,
            @RequestParam(value = "retainImageIds", required = false) List<String> retainImageIds,
            @RequestParam(value = "newImageClientIds", required = false) List<String> newImageClientIds,
            @RequestParam(value = "imageOrder", required = false) List<String> imageOrder,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages
    ) throws JsonProcessingException {
        MerchantAdminProductUpsertRequest request = objectMapper.readValue(payload, MerchantAdminProductUpsertRequest.class);
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        return merchantAdminProductService.updateProduct(actor, productId, request, retainImageIds, newImageClientIds, newImages, imageOrder);
    }

    @PostMapping(value = "/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public MerchantAdminProductDetailResponse updateProductViaPost(
            @PathVariable String productId,
            @RequestParam("payload") String payload,
            @RequestParam(value = "retainImageIds", required = false) List<String> retainImageIds,
            @RequestParam(value = "newImageClientIds", required = false) List<String> newImageClientIds,
            @RequestParam(value = "imageOrder", required = false) List<String> imageOrder,
            @RequestPart(value = "newImages", required = false) List<MultipartFile> newImages
    ) throws JsonProcessingException {
        MerchantAdminProductUpsertRequest request = objectMapper.readValue(payload, MerchantAdminProductUpsertRequest.class);
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        return merchantAdminProductService.updateProduct(actor, productId, request, retainImageIds, newImageClientIds, newImages, imageOrder);
    }

    @PostMapping("/{productId}/deactivate")
    public MerchantAdminProductDetailResponse deactivateProduct(
            @PathVariable String productId,
            @Valid @RequestBody MerchantAdminProductDeactivateRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        return merchantAdminProductService.deactivateProduct(actor, productId, request);
    }

    @PostMapping("/{productId}/activate")
    public MerchantAdminProductDetailResponse activateProduct(
            @PathVariable String productId,
            @Valid @RequestBody MerchantAdminProductActivateRequest request
    ) {
        AdminSessionActor actor = adminAccessPolicy.requireMerchantAdmin();
        adminAccessPolicy.checkMerchantScope(actor, request.merchantId());
        return merchantAdminProductService.activateProduct(actor, productId, request);
    }

    private String resolveMerchantId(AdminSessionActor actor, String merchantId) {
        if (merchantId != null && !merchantId.isBlank()) {
            adminAccessPolicy.checkMerchantScope(actor, merchantId);
            return merchantId;
        }
        if (actor.merchantIds().isEmpty()) {
            throw new BusinessException("MERCHANT_SCOPE_FORBIDDEN", "当前后台用户无权访问该商家数据", HttpStatus.FORBIDDEN);
        }
        return actor.merchantIds().get(0);
    }
}
