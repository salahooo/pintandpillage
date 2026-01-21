jest.mock('../../src/assets/ui-items/Spear.png', () => 'spear-icon');
jest.mock('../../src/assets/ui-items/Axe.png', () => 'axe-icon');

import { createLocalVue, mount } from '@vue/test-utils';
import Vuex from 'vuex';
import SmithModal from '@/components/ui/modals/SmithModal.vue';
import ResearchBox from '@/components/ui/ResearchBox.vue';

const localVue = createLocalVue();
localVue.use(Vuex);
localVue.component('research-box', ResearchBox);

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

const createResearch = (overrides = {}) => ({
    researchName: 'Spear',
    researchLevel: 0,
    baseSecondsToResearch: 10,
    secondsToResearch: '00:00:10',
    buildingLevelRequirement: 1,
    resourcesRequiredToResearch: { Wood: 100, Stone: 50 },
    ...overrides,
});

describe('Smith research building', () => {
    it('shouldStartResearchWhenButtonClicked', async () => {
        // Arrange: build a Smith modal with enough resources and a single research option.
        const dispatchSpy = jest.fn(() => Promise.resolve());
        const toasterSpy = { success: jest.fn() };
        const building = {
            buildingId: 7,
            name: 'Smith',
            level: 3,
            isResearchInProgress: false,
            currentResearch: null,
            researchTimeLeft: '00:00:00',
        };
        const spearResearch = createResearch();
        const village = {
            availableResearches: [spearResearch],
            villageResources: { Wood: 500, Stone: 400, Hop: 40, Beer: 200 },
        };
        const store = {
            getters: {
                building: jest.fn(() => building),
                village,
                resources: village.villageResources,
            },
            dispatch: dispatchSpy,
        };

        const wrapper = mount(ResearchBox, {
            localVue,
            propsData: {
                research: spearResearch,
                buildingLevel: 3,
                buildingId: 7,
                building,
            },
            mocks: {
                $store: store,
                $toaster: toasterSpy,
            },
            stubs: {
                'resource-item': true,
            },
        });

        // Act: click Start Research and wait for the mocked dispatch promise.
        await wrapper.find('button.researchButton').trigger('click');
        await flushPromises();

        // Assert: Vuex action is called with the correct payload and a toast is shown.
        expect(dispatchSpy).toHaveBeenCalledWith('startResearch', {
            researchBuildingId: 7,
            researchType: 'Spear',
        });
        expect(toasterSpy.success).toHaveBeenCalledWith('Research Started!');

        // Simulate backend state: the research is now in progress so the button is replaced.
        const updatedBuilding = {
            ...building,
            isResearchInProgress: true,
            currentResearch: spearResearch,
        };
        await wrapper.setProps({ building: updatedBuilding });
        expect(wrapper.find('button.researchButton').exists()).toBe(false);
        expect(wrapper.text()).toContain('Research under progress');
    });

    it('shouldShowNewResearchWhenListUpdates', async () => {
        // Arrange: start with one research option rendered by the Smith modal.
        const dispatchSpy = jest.fn(() => Promise.resolve());
        const building = {
            buildingId: 8,
            name: 'Smith',
            level: 3,
            isResearchInProgress: false,
            currentResearch: null,
            researchTimeLeft: '00:00:00',
        };
        const spearResearch = createResearch();
        const village = {
            availableResearches: [spearResearch],
            villageResources: { Wood: 500, Stone: 400, Hop: 40, Beer: 200 },
        };
        const store = {
            getters: {
                building: jest.fn(() => building),
                village,
                resources: village.villageResources,
            },
            dispatch: dispatchSpy,
        };

        const wrapper = mount(SmithModal, {
            localVue,
            propsData: { buildingId: 8 },
            mocks: { $store: store, $toaster: { success: jest.fn() } },
            stubs: {
                'level-up-building': true,
                'resource-item': true,
            },
        });

        // Act: append a new research option to mimic a mocked server update.
        village.availableResearches.push(
            createResearch({ researchName: 'Axe', buildingLevelRequirement: 3 })
        );
        wrapper.vm.$forceUpdate();
        await wrapper.vm.$nextTick();

        // Assert: the new research name is rendered in the list.
        expect(wrapper.text()).toContain('Axe');
    });
});
