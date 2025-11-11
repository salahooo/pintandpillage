jest.mock('../../src/assets/tiles/level_10/house_10.png', () => 'house-level-10');
jest.mock('../../src/assets/tiles/house.png', () => 'house-default');
jest.mock('../../src/assets/winterTiles/house.png', () => 'house-winter-default');
jest.mock('../../src/assets/tiles/level_10/winter/house_10.png', () => 'house-winter-level-10');

import { createLocalVue, shallowMount } from '@vue/test-utils';
import Vuex from 'vuex';
import Village from '@/views/Village.vue';
import BuildingListItem from '@/components/ui/BuildingListItem.vue';
import LevelUpBuilding from '@/components/ui/LevelUpBuilding.vue';
import House from '@/components/tiles/House.vue';

const localVue = createLocalVue();
localVue.use(Vuex);
localVue.component('router-link', {
    props: ['to'],
    render(h) {
        return h('a', this.$slots.default);
    },
});

const flushPromises = () => new Promise(resolve => setTimeout(resolve, 0));

const modalStubs = {
    modal: { template: '<div class="build-modal" />' },
    'settings-modal': true,
    'tutorial-modal': { template: '<div data-testid="tutorial-modal">Intro copy</div>' },
    'logs-modal': true,
    'combat-logs-modal': true,
    'villagegrid-component': { template: '<div class="grid-stub" />' },
};

describe('Onboarding and build entry points', () => {
    it('shows the tutorial introduction for first-time logins so players get guidance', () => {
        // Arrange: simulate a first login by keeping the Vuex getter true.
        const wrapper = shallowMount(Village, {
            localVue,
            mocks: { $store: { getters: { firstLogin: true } } },
            stubs: modalStubs,
        });

        // Assert: the tutorial modal (which contains the introduction text) must be rendered.
        expect(wrapper.find('[data-testid="tutorial-modal"]').exists()).toBe(true);
    });

    it('hides the tutorial after the first login so returning players are not blocked', () => {
        // Arrange: mimic a returning player by setting the getter to false.
        const wrapper = shallowMount(Village, {
            localVue,
            mocks: { $store: { getters: { firstLogin: false } } },
            stubs: modalStubs,
        });

        // Assert: without the first-login flag the introduction modal should disappear.
        expect(wrapper.find('[data-testid="tutorial-modal"]').exists()).toBe(false);
    });

    it('opens the build modal with the correct payload when a tile is clicked', async () => {
        // Arrange: prepare the village view and fake tile data to mimic a tile click event.
        const wrapper = shallowMount(Village, {
            localVue,
            mocks: { $store: { getters: { firstLogin: false } } },
            stubs: modalStubs,
        });
        const tileData = {
            name: 'House',
            buildingId: 42,
            allowedBuilding: 'House',
            position: { x: 3, y: 7 },
        };

        // Act: call the same handler that the tile click would trigger.
        wrapper.vm.showModal(tileData);
        await wrapper.vm.$nextTick();

        // Assert: the build modal becomes visible and receives the selected tile context.
        expect(wrapper.find('.build-modal').exists()).toBe(true);
        expect(wrapper.vm.isModalVisible).toBe(true);
        expect(wrapper.vm.buildingName).toBe('House');
        expect(wrapper.vm.position).toEqual(tileData.position);
    });
});

describe('Building interactions and upgrades', () => {
    it('dispatches a build action for houses so population can increase', async () => {
        // Arrange: set up the build menu entry with sufficient resources to allow construction.
        const dispatchSpy = jest.fn(() => Promise.resolve());
        const mockVillage = {
            villageId: 7,
            villageResources: { Wood: 250, Stone: 180 },
            populationLeft: 5,
            buildings: [],
        };
        const wrapper = shallowMount(BuildingListItem, {
            localVue,
            propsData: {
                building: {
                    name: 'House',
                    description: 'Adds population capacity',
                    resourcesRequiredLevelUp: { Wood: 200, Stone: 100 },
                    populationRequiredNextLevel: 2,
                    buildingLevelRequiredToLevelup: {},
                },
                position: { x: 1, y: 2 },
            },
            mocks: {
                $store: {
                    getters: { village: mockVillage },
                    dispatch: dispatchSpy,
                },
            },
            stubs: {
                'population-frame': true,
                'time-frame': true,
                'resource-item': true,
            },
        });

        // Act: trigger the Build button exactly like a player selecting "House".
        await wrapper.find('button').trigger('click');
        await flushPromises();

        // Assert: the component asks Vuex to create the house and closes the menu afterwards.
        expect(dispatchSpy).toHaveBeenCalledWith('createBuilding', {
            villageId: 7,
            buildingType: 'House',
            position: { x: 1, y: 2 },
        });
        expect(wrapper.emitted('close')).toBeTruthy();
    });

    it('upgrades resource buildings through the LevelUpBuilding flow to boost production', async () => {
        // Arrange: feed the upgrade widget with a resource building that has enough requirements met.
        const dispatchSpy = jest.fn(() => Promise.resolve());
        const buildingGetter = jest.fn(() => ({
            buildingId: 99,
            name: 'Lumberyard',
            level: 3,
            resourcesRequiredLevelUp: { Wood: 100 },
            populationRequiredNextLevel: 2,
            isUnderConstruction: false,
            constructionTime: 120,
        }));
        const wrapper = shallowMount(LevelUpBuilding, {
            localVue,
            propsData: { buildingId: 99 },
            mocks: {
                $store: {
                    getters: { building: buildingGetter },
                    state: {
                        village: {
                            data: {
                                villageResources: { Wood: 999 },
                                populationLeft: 10,
                            },
                        },
                    },
                    dispatch: dispatchSpy,
                    $dialog: { confirm: jest.fn() },
                },
            },
            stubs: {
                'population-frame': true,
                'time-frame': true,
                'resource-item': true,
            },
        });

        // Act: click the Level Up button to start the upgrade request.
        const levelUpButton = wrapper.findAll('button').wrappers.find(btn => btn.text() === 'Level Up');
        await levelUpButton.trigger('click');
        await flushPromises();

        // Assert: the upgrade action fires with the current building id and the modal closes afterwards.
        expect(dispatchSpy).toHaveBeenCalledWith('updateBuilding', 99);
        expect(wrapper.emitted('close')).toBeTruthy();
    });

    it('switches to the special level-10 artwork for houses to visualize progression', async () => {
        // Arrange: mount the house tile with mocked assets so we can detect which sprite is chosen.
        const wrapper = shallowMount(House, {
            localVue,
            propsData: {
                buildingProperties: { level: 10, isUnderConstruction: false },
            },
            mocks: {
                $store: {
                    state: { seasonsEnabled: false, currentSeason: 'summer' },
                },
            },
            stubs: {
                'construction-tile': true,
            },
        });

        // Act: capture the texture for level 10 and then downgrade the building to compare.
        const levelTenTexture = wrapper.vm.getTileSource();
        await wrapper.setProps({ buildingProperties: { level: 5, isUnderConstruction: false } });
        const normalTexture = wrapper.vm.getTileSource();

        // Assert: level 10 picks the dedicated asset, proving the visual upgrade is wired.
        expect(levelTenTexture).toBe('house-level-10');
        expect(normalTexture).toBe('house-default');
    });
});
