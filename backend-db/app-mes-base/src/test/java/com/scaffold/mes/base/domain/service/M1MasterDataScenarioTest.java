package com.scaffold.mes.base.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.baomidou.mybatisplus.extension.plugins.pagination.PageDTO;
import com.scaffold.mes.base.domain.MesBaseErrorCodes;
import com.scaffold.mes.base.domain.dto.AreaSaveRequest;
import com.scaffold.mes.base.domain.dto.CountUnitSaveRequest;
import com.scaffold.mes.base.domain.dto.EquipmentGroupSaveRequest;
import com.scaffold.mes.base.domain.dto.EquipmentSaveRequest;
import com.scaffold.mes.base.domain.dto.MaterialSaveRequest;
import com.scaffold.mes.base.domain.dto.ProductSaveRequest;
import com.scaffold.mes.base.domain.dto.ProductUnitConvSaveRequest;
import com.scaffold.mes.base.domain.dto.RecipeSaveRequest;
import com.scaffold.mes.base.domain.dto.RecipeVersionSaveRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionDecisionRequest;
import com.scaffold.mes.base.domain.dto.RouteVersionSubmitRequest;
import com.scaffold.mes.base.domain.dto.StepSaveRequest;
import com.scaffold.mes.base.domain.dto.UomSaveRequest;
import com.scaffold.mes.base.domain.entity.ApprovalRecord;
import com.scaffold.mes.base.domain.entity.Area;
import com.scaffold.mes.base.domain.entity.CountUnit;
import com.scaffold.mes.base.domain.entity.Equipment;
import com.scaffold.mes.base.domain.entity.EquipmentGroup;
import com.scaffold.mes.base.domain.entity.EquipmentStatusHist;
import com.scaffold.mes.base.domain.entity.Material;
import com.scaffold.mes.base.domain.entity.Product;
import com.scaffold.mes.base.domain.entity.ProductUnitConv;
import com.scaffold.mes.base.domain.entity.Recipe;
import com.scaffold.mes.base.domain.entity.RecipeVersion;
import com.scaffold.mes.base.domain.entity.Step;
import com.scaffold.mes.base.domain.entity.Uom;
import com.scaffold.mes.base.domain.repository.ApprovalRecordRepository;
import com.scaffold.mes.base.domain.repository.AreaRepository;
import com.scaffold.mes.base.domain.repository.CountUnitRepository;
import com.scaffold.mes.base.domain.repository.CustomerRepository;
import com.scaffold.mes.base.domain.repository.EquipmentGroupRepository;
import com.scaffold.mes.base.domain.repository.EquipmentPortRepository;
import com.scaffold.mes.base.domain.repository.EquipmentRepository;
import com.scaffold.mes.base.domain.repository.EquipmentStatusHistRepository;
import com.scaffold.mes.base.domain.repository.MaterialRepository;
import com.scaffold.mes.base.domain.repository.ProductFamilyRepository;
import com.scaffold.mes.base.domain.repository.ProductRepository;
import com.scaffold.mes.base.domain.repository.ProductUnitConvRepository;
import com.scaffold.mes.base.domain.repository.RecipeRepository;
import com.scaffold.mes.base.domain.repository.RecipeVersionRepository;
import com.scaffold.mes.base.domain.repository.StepGroupRepository;
import com.scaffold.mes.base.domain.repository.StepRepository;
import com.scaffold.mes.base.domain.repository.UomRepository;
import com.scaffold.share.exception.BusinessException;

/**
 * M1 验收：一套主数据能串起来。1 Wafer = 1200 Die，被引用的计数单位不能删。
 * <p>
 * 仓储是内存实现，不连数据库。
 */
class M1MasterDataScenarioTest {

    private final AtomicLong ids = new AtomicLong(100);
    private final Units units = new Units();
    private final Products products = new Products();
    private final Convs convs = new Convs();
    private final Areas areas = new Areas();
    private final Steps steps = new Steps();
    private final Groups groups = new Groups();
    private final Equipments equipments = new Equipments();
    private final Hists hists = new Hists();
    private final Uoms uoms = new Uoms();
    private final Materials materials = new Materials();
    private final Recipes recipes = new Recipes();
    private final Versions versions = new Versions();

    @Test
    void buildsOneMasterDataSet() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-16T02:00:00Z"), ZoneOffset.ofHours(8));
        CountUnitApplicationService countUnits = new CountUnitApplicationService(units);
        ProductApplicationService productService = new ProductApplicationService(products, units,
                mock(ProductFamilyRepository.class), mock(CustomerRepository.class));
        ProductUnitConvApplicationService convService = new ProductUnitConvApplicationService(convs, products, units);
        AreaApplicationService areaService = new AreaApplicationService(areas);
        StepApplicationService stepService = new StepApplicationService(steps, areas, mock(StepGroupRepository.class));
        EquipmentGroupApplicationService groupService = new EquipmentGroupApplicationService(groups, areas);
        EquipmentStatusApplicationService statusService = new EquipmentStatusApplicationService(equipments,
                mock(EquipmentPortRepository.class), hists, clock);
        EquipmentApplicationService equipmentService = new EquipmentApplicationService(equipments, groups, areas, statusService);
        UomApplicationService uomService = new UomApplicationService(uoms);
        MaterialApplicationService materialService = new MaterialApplicationService(materials, uoms);
        RecipeApplicationService recipeService = new RecipeApplicationService(recipes);
        ApprovalRecordRepository approvalRepository = mock(ApprovalRecordRepository.class);
        ApprovalRecord recipeSubmit = new ApprovalRecord();
        recipeSubmit.setAssignee("sam");
        when(approvalRepository.findLatest(eq("RECIPE_VERSION"), any(), eq("SUBMIT"))).thenReturn(Optional.of(recipeSubmit));
        ConfirmGateApplicationService confirms = mock(ConfirmGateApplicationService.class);
        when(confirms.allowed(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any())).thenReturn(true);
        RecipeVersionApplicationService versionService = new RecipeVersionApplicationService(versions, recipes,
                mock(ApprovalRecordApplicationService.class), approvalRepository, confirms, clock);

        countUnits.create(unit("WAFER", "晶圆"));
        countUnits.create(unit("DIE", "晶粒"));
        CountUnit wafer = units.byCode("WAFER");
        CountUnit die = units.byCode("DIE");

        productService.create(product(wafer.getId()));
        Product product = products.byCode("P-300");
        convService.create(product.getId(), conv(wafer.getId(), die.getId()));

        var within = convService.check(product.getId(), wafer.getId(), die.getId(), BigDecimal.ONE, new BigDecimal("1200"));
        var outside = convService.check(product.getId(), wafer.getId(), die.getId(), BigDecimal.ONE, new BigDecimal("1000"));
        assertThat(within.referenceQty()).isEqualByComparingTo("1200");
        assertThat(within.withinTolerance()).isTrue();
        assertThat(outside.withinTolerance()).isFalse();

        areaService.create(area());
        Area bay = areas.byCode("BAY-1");
        stepService.create(step(bay.getId()));
        Step photo = steps.byCode("PHOTO");
        assertThat(photo.getAreaId()).isEqualTo(bay.getId());
        assertThat(photo.getNeedEquipment()).isEqualTo(1);

        groupService.create(group(bay.getId()));
        EquipmentGroup litho = groups.byCode("LITHO");
        equipmentService.create(equipment(litho.getId(), bay.getId()));
        Equipment tool = equipments.byCode("EQ-1");
        assertThat(tool.getE10State()).isEqualTo("STANDBY");
        assertThat(hists.listOpen(tool.getId(), 0L)).hasSize(1);

        uomService.create(uom());
        materialService.create(material(uoms.byCode("KG").getId()));
        assertThat(materials.byCode("RESIN").getBaseUomId()).isEqualTo(uoms.byCode("KG").getId());

        recipeService.create(recipe());
        Recipe recipe = recipes.byCode("R-PHOTO");
        versionService.create(recipe.getId(), version());
        RecipeVersion draft = versions.rows.get(0);
        RouteVersionSubmitRequest submit = new RouteVersionSubmitRequest();
        submit.setAssignee("sam");
        versionService.submit(recipe.getId(), draft.getId(), "lee", submit);
        RouteVersionDecisionRequest decision = new RouteVersionDecisionRequest();
        decision.setOperator("sam");
        versionService.approve(recipe.getId(), draft.getId(), decision);
        assertThat(versions.rows.stream().filter(row -> "ACTIVE".equals(row.getStatus()))).hasSize(1);

        assertThatThrownBy(() -> countUnits.delete(wafer.getId()))
                .extracting(ex -> ((BusinessException) ex).getCode())
                .isEqualTo(MesBaseErrorCodes.COUNT_UNIT_IN_USE);
    }

    private long nextId() {
        return ids.incrementAndGet();
    }

    private static CountUnitSaveRequest unit(String code, String name) {
        CountUnitSaveRequest request = new CountUnitSaveRequest();
        request.setUnitCode(code);
        request.setUnitName(name);
        request.setUnitType(code);
        return request;
    }

    private static ProductSaveRequest product(Long baseUnitId) {
        ProductSaveRequest request = new ProductSaveRequest();
        request.setProductCode("P-300");
        request.setProductName("300mm 逻辑片");
        request.setBaseUnitId(baseUnitId);
        request.setWaferSizeMm(300);
        return request;
    }

    private static ProductUnitConvSaveRequest conv(Long from, Long to) {
        ProductUnitConvSaveRequest request = new ProductUnitConvSaveRequest();
        request.setFromUnitId(from);
        request.setToUnitId(to);
        request.setFactor(new BigDecimal("1200"));
        return request;
    }

    private static AreaSaveRequest area() {
        AreaSaveRequest request = new AreaSaveRequest();
        request.setAreaCode("BAY-1");
        request.setAreaName("光刻湾");
        return request;
    }

    private static StepSaveRequest step(Long areaId) {
        StepSaveRequest request = new StepSaveRequest();
        request.setStepCode("PHOTO");
        request.setStepName("曝光");
        request.setStepType("PROCESS");
        request.setAreaId(areaId);
        return request;
    }

    private static EquipmentGroupSaveRequest group(Long areaId) {
        EquipmentGroupSaveRequest request = new EquipmentGroupSaveRequest();
        request.setGroupCode("LITHO");
        request.setGroupName("光刻组");
        request.setAreaId(areaId);
        return request;
    }

    private static EquipmentSaveRequest equipment(Long groupId, Long areaId) {
        EquipmentSaveRequest request = new EquipmentSaveRequest();
        request.setEquipmentCode("EQ-1");
        request.setEquipmentName("光刻机");
        request.setGroupId(groupId);
        request.setAreaId(areaId);
        return request;
    }

    private static UomSaveRequest uom() {
        UomSaveRequest request = new UomSaveRequest();
        request.setUomCode("KG");
        request.setUomName("千克");
        request.setDimension("WEIGHT");
        return request;
    }

    private static MaterialSaveRequest material(Long uomId) {
        MaterialSaveRequest request = new MaterialSaveRequest();
        request.setMaterialCode("RESIN");
        request.setMaterialName("树脂");
        request.setBaseUomId(uomId);
        return request;
    }

    private static RecipeSaveRequest recipe() {
        RecipeSaveRequest request = new RecipeSaveRequest();
        request.setRecipeCode("R-PHOTO");
        request.setRecipeName("曝光程序");
        request.setEquipmentType("PROCESS");
        return request;
    }

    private static RecipeVersionSaveRequest version() {
        RecipeVersionSaveRequest request = new RecipeVersionSaveRequest();
        request.setParamDesc("{\"exp\":10}");
        return request;
    }

    private final class Units implements CountUnitRepository {
        private final List<CountUnit> rows = new ArrayList<>();

        private CountUnit byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<CountUnit> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<CountUnit> findByCode(String unitCode) {
            return rows.stream().filter(row -> unitCode.equals(row.getUnitCode())).findFirst();
        }

        @Override
        public PageDTO<CountUnit> page(int page, int pageSize, String unitCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(CountUnit unit) {
            unit.setId(nextId());
            rows.add(unit);
        }

        @Override
        public void update(CountUnit unit) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            rows.removeIf(row -> id.equals(row.getId()));
        }

        @Override
        public long countReferences(Long id) {
            long usedByProduct = products.rows.stream().filter(row -> id.equals(row.getBaseUnitId())).count();
            long usedByConv = convs.rows.stream()
                    .filter(row -> id.equals(row.getFromUnitId()) || id.equals(row.getToUnitId()))
                    .count();
            return usedByProduct + usedByConv;
        }
    }

    private final class Products implements ProductRepository {
        private final List<Product> rows = new ArrayList<>();

        private Product byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Product> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Product> findByCode(String productCode) {
            return rows.stream().filter(row -> productCode.equals(row.getProductCode())).findFirst();
        }

        @Override
        public PageDTO<Product> page(int page, int pageSize, String productCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Product product) {
            product.setId(nextId());
            rows.add(product);
        }

        @Override
        public void update(Product product) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }

        @Override
        public void lock(Long id) {
            if (findById(id).isEmpty()) {
                throw new BusinessException(MesBaseErrorCodes.PRODUCT_NOT_FOUND, "产品不存在");
            }
        }
    }

    private final class Convs implements ProductUnitConvRepository {
        private final List<ProductUnitConv> rows = new ArrayList<>();

        @Override
        public Optional<ProductUnitConv> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<ProductUnitConv> findExact(Long productId, Long fromUnitId, Long toUnitId) {
            return rows.stream()
                    .filter(row -> productId.equals(row.getProductId())
                            && fromUnitId.equals(row.getFromUnitId())
                            && toUnitId.equals(row.getToUnitId()))
                    .findFirst();
        }

        @Override
        public PageDTO<ProductUnitConv> page(int page, int pageSize, Long productId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(ProductUnitConv conversion) {
            conversion.setId(nextId());
            rows.add(conversion);
        }

        @Override
        public void update(ProductUnitConv conversion) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }
    }

    private final class Areas implements AreaRepository {
        private final List<Area> rows = new ArrayList<>();

        private Area byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Area> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Area> findByCode(String areaCode) {
            return rows.stream().filter(row -> areaCode.equals(row.getAreaCode())).findFirst();
        }

        @Override
        public PageDTO<Area> page(int page, int pageSize, String areaCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Area area) {
            area.setId(nextId());
            rows.add(area);
        }

        @Override
        public void update(Area area) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Steps implements StepRepository {
        private final List<Step> rows = new ArrayList<>();

        private Step byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Step> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Step> findByCode(String stepCode) {
            return rows.stream().filter(row -> stepCode.equals(row.getStepCode())).findFirst();
        }

        @Override
        public PageDTO<Step> page(int page, int pageSize, String stepCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Step step) {
            step.setId(nextId());
            rows.add(step);
        }

        @Override
        public void update(Step step) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Groups implements EquipmentGroupRepository {
        private final List<EquipmentGroup> rows = new ArrayList<>();

        private EquipmentGroup byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<EquipmentGroup> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<EquipmentGroup> findByCode(String groupCode) {
            return rows.stream().filter(row -> groupCode.equals(row.getGroupCode())).findFirst();
        }

        @Override
        public PageDTO<EquipmentGroup> page(int page, int pageSize, String groupCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(EquipmentGroup group) {
            group.setId(nextId());
            rows.add(group);
        }

        @Override
        public void update(EquipmentGroup group) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Equipments implements EquipmentRepository {
        private final List<Equipment> rows = new ArrayList<>();

        private Equipment byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Equipment> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Equipment> findByCode(String equipmentCode) {
            return rows.stream().filter(row -> equipmentCode.equals(row.getEquipmentCode())).findFirst();
        }

        @Override
        public PageDTO<Equipment> page(int page, int pageSize, String equipmentCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Equipment equipment) {
            equipment.setId(nextId());
            rows.add(equipment);
        }

        @Override
        public void update(Equipment equipment) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countByE10State(String e10State) {
            return rows.stream().filter(row -> e10State.equals(row.getE10State())).count();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Hists implements EquipmentStatusHistRepository {
        private final List<EquipmentStatusHist> rows = new ArrayList<>();

        @Override
        public List<EquipmentStatusHist> listOpen(Long equipmentId, Long portId) {
            return rows.stream()
                    .filter(row -> equipmentId.equals(row.getEquipmentId()) && portId.equals(row.getPortId())
                            && row.getEndAt() == null)
                    .toList();
        }

        @Override
        public PageDTO<EquipmentStatusHist> page(Long equipmentId, int page, int pageSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(EquipmentStatusHist hist) {
            hist.setId(nextId());
            rows.add(hist);
        }

        @Override
        public void update(EquipmentStatusHist hist) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteUntouchedInitial(Long equipmentId) {
            rows.removeIf(row -> equipmentId.equals(row.getEquipmentId()) && row.getEndAt() == null && row.getFromE10State() == null);
        }
    }

    private final class Uoms implements UomRepository {
        private final List<Uom> rows = new ArrayList<>();

        private Uom byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Uom> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Uom> findByCode(String uomCode) {
            return rows.stream().filter(row -> uomCode.equals(row.getUomCode())).findFirst();
        }

        @Override
        public PageDTO<Uom> page(int page, int pageSize, String uomCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Uom uom) {
            uom.setId(nextId());
            rows.add(uom);
        }

        @Override
        public void update(Uom uom) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Materials implements MaterialRepository {
        private final List<Material> rows = new ArrayList<>();

        private Material byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Material> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Material> findByCode(String materialCode) {
            return rows.stream().filter(row -> materialCode.equals(row.getMaterialCode())).findFirst();
        }

        @Override
        public PageDTO<Material> page(int page, int pageSize, String materialCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Material material) {
            material.setId(nextId());
            rows.add(material);
        }

        @Override
        public void update(Material material) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }
    }

    private final class Recipes implements RecipeRepository {
        private final List<Recipe> rows = new ArrayList<>();

        private Recipe byCode(String code) {
            return findByCode(code).orElseThrow();
        }

        @Override
        public Optional<Recipe> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public Optional<Recipe> findByCode(String recipeCode) {
            return rows.stream().filter(row -> recipeCode.equals(row.getRecipeCode())).findFirst();
        }

        @Override
        public PageDTO<Recipe> page(int page, int pageSize, String recipeCode) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void save(Recipe recipe) {
            recipe.setId(nextId());
            rows.add(recipe);
        }

        @Override
        public void update(Recipe recipe) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long countReferences(Long id) {
            return 0;
        }

        @Override
        public void lock(Long id) {
            if (findById(id).isEmpty()) {
                throw new BusinessException(MesBaseErrorCodes.RECIPE_NOT_FOUND, "Recipe 不存在");
            }
        }
    }

    private final class Versions implements RecipeVersionRepository {
        private final List<RecipeVersion> rows = new ArrayList<>();

        @Override
        public Optional<RecipeVersion> findById(Long id) {
            return rows.stream().filter(row -> id.equals(row.getId())).findFirst();
        }

        @Override
        public PageDTO<RecipeVersion> page(Long recipeId, int page, int pageSize) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<RecipeVersion> listActive(Long recipeId) {
            return rows.stream()
                    .filter(row -> recipeId.equals(row.getRecipeId()) && "ACTIVE".equals(row.getStatus()))
                    .toList();
        }

        @Override
        public int nextVersionNo(Long recipeId) {
            return rows.stream()
                    .filter(row -> recipeId.equals(row.getRecipeId()))
                    .map(RecipeVersion::getVersionNo)
                    .max(Integer::compareTo)
                    .orElse(0) + 1;
        }

        @Override
        public void save(RecipeVersion version) {
            version.setId(nextId());
            rows.add(version);
        }

        @Override
        public void update(RecipeVersion version) {
        }

        @Override
        public void deleteById(Long id) {
            throw new UnsupportedOperationException();
        }
    }
}
