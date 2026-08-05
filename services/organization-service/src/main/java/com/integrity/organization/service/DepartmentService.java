package com.integrity.organization.service;

import com.integrity.exception.NotFoundException;
import com.integrity.organization.domain.Department;
import com.integrity.organization.repository.DepartmentRepository;
import com.integrity.organization.web.dto.CreateDepartmentRequest;
import com.integrity.organization.web.dto.DepartmentResponse;
import com.integrity.organization.web.dto.UpdateDepartmentRequest;
import java.util.UUID;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/** CRUD operations over the tenant department tree. */
public final class DepartmentService {

  private final DepartmentRepository departmentRepository;
  private final OrganizationMapper mapper;

  /** Creates a service bound to the given repository. */
  public DepartmentService(DepartmentRepository departmentRepository, OrganizationMapper mapper) {
    this.departmentRepository = departmentRepository;
    this.mapper = mapper;
  }

  /** Creates a department, optionally under a parent of the same organization. */
  public Mono<DepartmentResponse> createDepartment(
      UUID organizationId, UUID byUser, CreateDepartmentRequest request) {
    return validateParent(organizationId, request.parentId())
        .then(
            departmentRepository
                .save(
                    new Department(
                        organizationId, request.parentId(), request.name().trim(), byUser))
                .map(mapper::toResponse));
  }

  /** Lists the departments of the organization. */
  public Flux<DepartmentResponse> listDepartments(UUID organizationId) {
    return departmentRepository.listLiveByOrganization(organizationId).map(mapper::toResponse);
  }

  /** Returns a single department. */
  public Mono<DepartmentResponse> getDepartment(UUID organizationId, UUID departmentId) {
    return requireDepartment(organizationId, departmentId).map(mapper::toResponse);
  }

  /** Renames a department. */
  public Mono<DepartmentResponse> renameDepartment(
      UUID organizationId, UUID departmentId, UUID byUser, UpdateDepartmentRequest request) {
    return requireDepartment(organizationId, departmentId)
        .flatMap(
            department -> {
              department.rename(request.name().trim(), byUser);
              return departmentRepository.save(department);
            })
        .map(mapper::toResponse);
  }

  /** Soft deletes a department. */
  public Mono<Void> deleteDepartment(UUID organizationId, UUID departmentId, UUID byUser) {
    return requireDepartment(organizationId, departmentId)
        .flatMap(
            department -> {
              department.delete(byUser);
              return departmentRepository.save(department).then();
            });
  }

  private Mono<Void> validateParent(UUID organizationId, UUID parentId) {
    if (parentId == null) {
      return Mono.empty();
    }
    return requireDepartment(organizationId, parentId).then();
  }

  private Mono<Department> requireDepartment(UUID organizationId, UUID departmentId) {
    return departmentRepository
        .findLiveById(departmentId)
        .switchIfEmpty(Mono.error(new NotFoundException("Department not found")))
        .flatMap(
            department -> {
              if (!department.getOrganizationId().equals(organizationId)) {
                return Mono.error(new NotFoundException("Department not found"));
              }
              return Mono.just(department);
            });
  }
}
