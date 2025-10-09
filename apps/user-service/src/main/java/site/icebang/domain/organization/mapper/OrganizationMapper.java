package site.icebang.domain.organization.mapper;

import java.math.BigInteger;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import site.icebang.domain.department.dto.DepartmentCardDto;
import site.icebang.domain.organization.dto.OrganizationCardDto;
import site.icebang.domain.position.dto.PositionCardDto;
import site.icebang.domain.roles.dto.RoleCardDto;

@Mapper
public interface OrganizationMapper {
  List<OrganizationCardDto> findAllOrganizations();

  List<DepartmentCardDto> findDepartmentsByOrganizationId(
      @Param("organizationId") BigInteger organizationId);

  List<PositionCardDto> findPositionsByOrganizationId(
      @Param("organizationId") BigInteger organizationId);

  List<RoleCardDto> findRolesByOrganizationId(@Param("organizationId") BigInteger organizationId);
}
