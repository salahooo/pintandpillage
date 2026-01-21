jest.mock('../../src/assets/ui-items/Spear.png', () => 'spear-icon');

import { createLocalVue, shallowMount } from '@vue/test-utils';
import Vuex from 'vuex';
import Unit from '@/components/ui/barracks/Unit.vue';

const localVue = createLocalVue();
localVue.use(Vuex);

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

const createUnitData = (overrides = {}) => ({
    unit: {
        unitName: 'Spear',
        description: 'Basic unit with spear',
        attack: 15,
        defence: 15,
        health: 20,
        speed: 10,
        populationRequiredPerUnit: 1,
        baseTimeToProduce: '00:00:10',
        resourcesRequiredToProduce: { Wood: 15, Beer: 15 },
        researchRequired: 'Spear',
    },
    levelUnlocked: 1,
    ...overrides,
});

const createStore = (overrides = {}) => {
    const baseVillage = {
        villageResources: { Wood: 200, Beer: 200 },
        populationLeft: 5,
        completedResearches: [{ researchName: 'Spear' }],
        unitsInVillage: [{ unit: { unitName: 'Spear' }, amount: 3 }],
    };
    return {
        getters: {
            village: baseVillage,
            ...overrides.getters,
        },
        dispatch: overrides.dispatch || jest.fn(() => Promise.resolve()),
    };
};

describe('Unit creation UI', () => {
    it('shouldDispatchCreateUnit_whenValidInput', async () => {
        // Arrange: enough resources, population, and research to train units.
        const dispatchSpy = jest.fn(() => Promise.resolve());
        const store = createStore({ dispatch: dispatchSpy });
        const toaster = { success: jest.fn(), error: jest.fn() };

        const wrapper = shallowMount(Unit, {
            localVue,
            propsData: {
                unit: createUnitData(),
                building: { buildingId: 99, name: 'Barracks', level: 3 },
                unitUnlockList: [{ unitType: 'Spear', level: 1 }],
            },
            mocks: {
                $store: store,
                $toaster: toaster,
            },
            stubs: {
                'resource-item': true,
                'population-frame': true,
                'time-frame': true,
            },
        });

        // Act: set a valid amount and click Train.
        await wrapper.setData({ sliderValue: 2 });
        await wrapper.find('button').trigger('click');
        await flushPromises();

        // Assert: the Vuex action is called with the correct payload.
        expect(dispatchSpy).toHaveBeenCalledWith('createUnit', {
            productionBuildingId: 99,
            unitType: 'Spear',
            amount: 2,
        });
        expect(toaster.success).toHaveBeenCalledWith('Unit production started!');
        expect(wrapper.vm.sliderValue).toBe(0);
    });

    it('shouldDisableTrain_whenResearchMissing', () => {
        // Arrange: remove research so the unit should be blocked.
        const store = createStore({
            getters: {
                village: {
                    villageResources: { Wood: 200, Beer: 200 },
                    populationLeft: 5,
                    completedResearches: [],
                    unitsInVillage: [],
                },
            },
        });

        const wrapper = shallowMount(Unit, {
            localVue,
            propsData: {
                unit: createUnitData(),
                building: { buildingId: 10, name: 'Barracks', level: 3 },
                unitUnlockList: [{ unitType: 'Spear', level: 1 }],
            },
            mocks: {
                $store: store,
                $toaster: { success: jest.fn(), error: jest.fn() },
            },
            stubs: {
                'resource-item': true,
                'population-frame': true,
                'time-frame': true,
            },
        });

        // Assert: the unit shows the research requirement and Train is disabled.
        expect(wrapper.text()).toContain('Unit requires research Spear');
        expect(wrapper.find('button').attributes('disabled')).toBe('disabled');
    });

    it('shouldDisableTrain_whenNotEnoughResources', () => {
        // Arrange: no resources so max produce should be zero.
        const store = createStore({
            getters: {
                village: {
                    villageResources: { Wood: 0, Beer: 0 },
                    populationLeft: 5,
                    completedResearches: [{ researchName: 'Spear' }],
                    unitsInVillage: [],
                },
            },
        });

        const wrapper = shallowMount(Unit, {
            localVue,
            propsData: {
                unit: createUnitData(),
                building: { buildingId: 11, name: 'Barracks', level: 3 },
                unitUnlockList: [{ unitType: 'Spear', level: 1 }],
            },
            mocks: {
                $store: store,
                $toaster: { success: jest.fn(), error: jest.fn() },
            },
            stubs: {
                'resource-item': true,
                'population-frame': true,
                'time-frame': true,
            },
        });

        // Assert: training is disabled when resources are insufficient.
        expect(wrapper.find('button').attributes('disabled')).toBe('disabled');
    });
});
