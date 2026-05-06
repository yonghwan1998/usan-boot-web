package com.usanmap.usan.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import com.usanmap.usan.dto.ListingRequest;
import com.usanmap.usan.security.SecurityUtils;
import com.usanmap.usan.service.AddressSearchService;
import com.usanmap.usan.service.ListingPhotoService;
import com.usanmap.usan.service.ListingService;
import com.usanmap.usan.service.storage.FileStorageService;
import com.usanmap.usan.service.storage.StoredFile;

import java.util.List;
import java.util.Map;

@RequestMapping("/listings")
@Controller
@RequiredArgsConstructor
public class ListingsController {

    private final AddressSearchService addressSearchService;
    private final FileStorageService fileStorageService;
    private final ListingService listingService;
    private final ListingPhotoService listingPhotoService;
    private final SecurityUtils securityUtils;

    /**
     * @date    2026-01-06
     * @author  yongss
     * @param   {}
     *
     * 처리 과정:
     *  - 매물 관리하기 페이지 이동
     */
    @GetMapping("")
    public String list(Model model) {
        Long userId = securityUtils.currentUserIdOrThrow();
        model.addAttribute("listings", listingService.getMyListings(userId));
        return "pages/listings/listings-list";
    }

    /**
     * @date    2025-12-24
     * @author  yongss
     * @param   {Model model}
     *
     * 처리 과정:
     *  - 폼 바인딩용 listingRequest에 초기화 데이터 담아서 전달
     */
    @GetMapping("/new")
    public String newPage(Model model) {
        model.addAttribute("listingRequest", ListingRequest.empty());
        model.addAttribute("listingStep", 1);

        return "pages/listings/listings-new";
    }

    /**
     * @date    2026-01-06
     * @author  yongss
     * @param   {ListingRequest listingRequest}
     *
     * 처리 과정:
     *  - 폼 바인딩용 listingRequest에 담겨 전달 된 데이터를 검증
     *  - 검증 완료 된 데이터 DB에 저장
     *  - 매물 관리하기 페이지로 리다이렉트
     */
    @PostMapping("")
    public String create(@Valid @ModelAttribute("listingRequest") ListingRequest listingRequest,
                         BindingResult bindingResult,
                         Model model,
                         @RequestParam(value = "photoFiles", required = false)List<MultipartFile> photoFiles) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("listingStep", 1);
            return "pages/listings/listings-new";
        }

        Long userId = securityUtils.currentUserIdOrThrow();

        var listing = listingService.create(listingRequest, userId);
        String publicId = listing.getPublicId();

        List<StoredFile> storedFiles = fileStorageService.storeListingPhotos(publicId, photoFiles);

        listingPhotoService.saveAll(listing.getId(), storedFiles);

        // TODO(yongss): 검증 완료 시 front에 status를 보내 매물 전송 or 매물 관리로 이동할 수 있게

        return "redirect:/listings";
    }

    /**
     * @date    2026-01-07
     * @author  yongss
     * @param   {}
     *
     * 처리 과정:
     *  - publicId로 매물 조회
     *  - 수정 폼 바인딩용 ListingRequest 세팅
     *  - 수정 페이지 렌더링
     */
    @DeleteMapping("/{publicId}")
    @ResponseBody
    public ResponseEntity<Void> delete(@PathVariable String publicId) {
        Long userId = securityUtils.currentUserIdOrThrow();
        listingService.delete(publicId, userId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{publicId}/edit")
    public String editPage(@PathVariable String publicId, Model model) {

        // TODO(yongss): publicId로 Listing 조회
        // TODO(yongss): 조회 결과를 ListingRequest로 매핑
        // TODO(yongss): 조회 실패 시 404 처리

        return "pages/listings/listings-edit";
    }

    @GetMapping("/api/address/search")
    @ResponseBody
    public Map<String, Object> searchAddress(@RequestParam("q") String q) {

        var items = addressSearchService.search(q);

        return Map.of("items", items);
    }

}
