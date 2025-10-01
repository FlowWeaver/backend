package site.icebang.domain.organization.controller;

import java.math.BigInteger;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import site.icebang.common.dto.ApiResponseDto;
import site.icebang.domain.organization.dto.OrganizationCardDto;
import site.icebang.domain.organization.dto.OrganizationOptionDto;
import site.icebang.domain.organization.service.OrganizationService;

@RequestMapping("/v0/organizations")
@RequiredArgsConstructor
@RestController
public class OrganizationController {
  private final OrganizationService organizationService;

  @GetMapping("")
  public ResponseEntity<ApiResponseDto<List<OrganizationCardDto>>> getOrganizations() {
    return ResponseEntity.ok(ApiResponseDto.success(organizationService.getAllOrganizationList()));
  }

  @GetMapping("/{id}/options")
  public ResponseEntity<ApiResponseDto<OrganizationOptionDto>> getOrganizationDetails(
      @PathVariable BigInteger id) {
    return ResponseEntity.ok(ApiResponseDto.success(organizationService.getOrganizationOptions(id)));
  }
}
