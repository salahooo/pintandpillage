package nl.duckstudios.pintandpillage;

import nl.duckstudios.pintandpillage.Exceptions.AttackingConditionsNotMetException;
import nl.duckstudios.pintandpillage.controller.CombatController;
import nl.duckstudios.pintandpillage.dao.TravelDao;
import nl.duckstudios.pintandpillage.entity.User;
import nl.duckstudios.pintandpillage.entity.Village;
import nl.duckstudios.pintandpillage.entity.VillageUnit;
import nl.duckstudios.pintandpillage.entity.travels.AttackCombatTravel;
import nl.duckstudios.pintandpillage.model.AttackUnitData;
import nl.duckstudios.pintandpillage.model.AttackVillageData;
import nl.duckstudios.pintandpillage.model.UnitType;
import nl.duckstudios.pintandpillage.helper.UnitFactory;
import nl.duckstudios.pintandpillage.service.AccountService;
import nl.duckstudios.pintandpillage.service.AuthenticationService;
import nl.duckstudios.pintandpillage.service.CombatService;
import nl.duckstudios.pintandpillage.service.DistanceService;
import nl.duckstudios.pintandpillage.service.VillageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AttackModalLogicTest {

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private AccountService accountService;

    @Mock
    private VillageService villageService;

    @Mock
    private CombatService combatService;

    @Mock
    private TravelDao travelDao;

    @Mock
    private DistanceService distanceService;

    @InjectMocks
    private CombatController combatController;

    @Test
    void shouldStartAttack_whenEnoughResourcesAndUnits() {
        // Arrange: a valid attack payload with enough units and ship capacity.
        AttackVillageData data = new AttackVillageData();
        data.fromVillageId = 1L;
        data.toVillageId = 2L;
        data.units = List.of(new AttackUnitData(UnitType.Spear, 5));

        User user = new User();
        user.setId(42L);

        Village attackingVillage = new Village();
        attackingVillage.setVillageId(1L);
        Village defendingVillage = new Village();
        defendingVillage.setVillageId(2L);

        List<VillageUnit> attackingUnits = List.of(
                new VillageUnit(UnitFactory.getUnitStatic(UnitType.Spear.name()), 5)
        );

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(villageService.getVillage(1L)).thenReturn(attackingVillage);
        when(villageService.getVillage(2L)).thenReturn(defendingVillage);
        when(combatService.convertToVillageUnits(data)).thenReturn(attackingUnits);
        when(distanceService.calculateDistance(any(), any())).thenReturn(5);

        // Act: attempt the attack.
        Village result = combatController.attackVillage(data);

        // Assert: the attack is accepted and a travel is stored.
        assertThat(result).isSameAs(attackingVillage);
        verify(accountService).checkIsCorrectUser(user.getId(), attackingVillage);
        verify(combatService).checkHasEnoughUnitsToAttack(attackingUnits, attackingVillage);
        verify(combatService).checkHasEnoughShipsToSendUnits(attackingUnits);
        verify(villageService).update(attackingVillage);

        ArgumentCaptor<AttackCombatTravel> travelCaptor = ArgumentCaptor.forClass(AttackCombatTravel.class);
        verify(travelDao).insertAttack(travelCaptor.capture());
        AttackCombatTravel travel = travelCaptor.getValue();
        assertThat(travel.getAttackingVillage()).isEqualTo(attackingVillage);
        assertThat(travel.getDefendingVillage()).isEqualTo(defendingVillage);
        assertThat(travel.getTravelingUnits()).isEqualTo(attackingUnits);
    }

    @Test
    void shouldFailAttack_whenNoUnitsPresent() {
        // Arrange: conversion fails because no units are selected.
        AttackVillageData data = new AttackVillageData();
        data.fromVillageId = 1L;
        data.toVillageId = 2L;
        data.units = List.of();

        User user = new User();
        user.setId(11L);
        Village attackingVillage = new Village();
        attackingVillage.setVillageId(1L);
        Village defendingVillage = new Village();
        defendingVillage.setVillageId(2L);

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(villageService.getVillage(1L)).thenReturn(attackingVillage);
        when(villageService.getVillage(2L)).thenReturn(defendingVillage);

        when(combatService.convertToVillageUnits(data))
                .thenThrow(new AttackingConditionsNotMetException("To attack you need to send at least one unit"));

        // Act + Assert: the error is propagated.
        assertThatThrownBy(() -> combatController.attackVillage(data))
                .isInstanceOf(AttackingConditionsNotMetException.class)
                .hasMessageContaining("at least one unit");
        verifyNoInteractions(travelDao);
    }

    @Test
    void shouldFailAttack_whenInsufficientUnits() {
        // Arrange: combat service rejects because units are insufficient.
        AttackVillageData data = new AttackVillageData();
        data.fromVillageId = 1L;
        data.toVillageId = 2L;
        data.units = List.of(new AttackUnitData(UnitType.Spear, 5));

        User user = new User();
        user.setId(12L);
        Village attackingVillage = new Village();
        attackingVillage.setVillageId(1L);
        Village defendingVillage = new Village();
        defendingVillage.setVillageId(2L);

        List<VillageUnit> attackingUnits = List.of(
                new VillageUnit(UnitFactory.getUnitStatic(UnitType.Spear.name()), 5)
        );

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(villageService.getVillage(1L)).thenReturn(attackingVillage);
        when(villageService.getVillage(2L)).thenReturn(defendingVillage);
        when(combatService.convertToVillageUnits(data)).thenReturn(attackingUnits);
        doThrow(new AttackingConditionsNotMetException("Not enough Spear to attack this village"))
                .when(combatService).checkHasEnoughUnitsToAttack(eq(attackingUnits), any(Village.class));

        // Act + Assert: the attack is rejected.
        assertThatThrownBy(() -> combatController.attackVillage(data))
                .isInstanceOf(AttackingConditionsNotMetException.class)
                .hasMessageContaining("Not enough Spear");
        verify(travelDao, never()).insertAttack(any());
    }

    @Test
    void shouldFailAttack_whenInsufficientResources() {
        // Arrange: ship capacity is treated as required attack resource.
        AttackVillageData data = new AttackVillageData();
        data.fromVillageId = 1L;
        data.toVillageId = 2L;
        data.units = List.of(new AttackUnitData(UnitType.Spear, 10));

        User user = new User();
        user.setId(13L);
        Village attackingVillage = new Village();
        attackingVillage.setVillageId(1L);
        Village defendingVillage = new Village();
        defendingVillage.setVillageId(2L);

        List<VillageUnit> attackingUnits = List.of(
                new VillageUnit(UnitFactory.getUnitStatic(UnitType.Spear.name()), 10)
        );

        when(authenticationService.getAuthenticatedUser()).thenReturn(user);
        when(villageService.getVillage(1L)).thenReturn(attackingVillage);
        when(villageService.getVillage(2L)).thenReturn(defendingVillage);
        when(combatService.convertToVillageUnits(data)).thenReturn(attackingUnits);
        doThrow(new AttackingConditionsNotMetException("Not enough ship capacity for this attack"))
                .when(combatService).checkHasEnoughShipsToSendUnits(attackingUnits);

        // Act + Assert: the lack of ship capacity blocks the attack.
        assertThatThrownBy(() -> combatController.attackVillage(data))
                .isInstanceOf(AttackingConditionsNotMetException.class)
                .hasMessageContaining("ship capacity");
        verify(travelDao, never()).insertAttack(any());
    }

    @Test
    void shouldFailAttack_whenTargetVillageDoesNotMeetAttackConditions() {
        // Arrange: attacking your own village is not allowed.
        AttackVillageData data = new AttackVillageData();
        data.fromVillageId = 1L;
        data.toVillageId = 1L;
        data.units = List.of(new AttackUnitData(UnitType.Spear, 1));

        // Act + Assert: the controller blocks self-attacks immediately.
        assertThatThrownBy(() -> combatController.attackVillage(data))
                .isInstanceOf(AttackingConditionsNotMetException.class)
                .hasMessageContaining("should not attack yourself");
        verifyNoInteractions(authenticationService, villageService, travelDao);
    }
}
