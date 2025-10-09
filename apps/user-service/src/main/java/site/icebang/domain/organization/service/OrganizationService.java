package site.icebang.domain.organization.service;

import java.math.BigInteger;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import site.icebang.domain.department.dto.DepartmentCardDto;
import site.icebang.domain.organization.dto.OrganizationCardDto;
import site.icebang.domain.organization.dto.OrganizationOptionDto;
import site.icebang.domain.organization.mapper.OrganizationMapper;
import site.icebang.domain.position.dto.PositionCardDto;
import site.icebang.domain.roles.dto.RoleCardDto;

@Service
@RequiredArgsConstructor
public class OrganizationService {
  private final OrganizationMapper organizationMapper;

  @Transactional(readOnly = true)
  public List<OrganizationCardDto> getAllOrganizationList() {
    return organizationMapper.findAllOrganizations();
  }

  public OrganizationOptionDto getOrganizationOptions(BigInteger id) {
    List<DepartmentCardDto> departments = organizationMapper.findDepartmentsByOrganizationId(id);
    List<PositionCardDto> positions = organizationMapper.findPositionsByOrganizationId(id);
    List<RoleCardDto> roles = organizationMapper.findRolesByOrganizationId(id);

    return OrganizationOptionDto.builder()
        .departments(departments)
        .positions(positions)
        .roles(roles)
        .build();
  }
}
