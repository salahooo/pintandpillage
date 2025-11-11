import { createLocalVue, shallowMount } from '@vue/test-utils';
import Vuex from 'vuex';
import ResourcesModal from '@/components/ui/modals/ResourcesModal.vue';

const localVue = createLocalVue();
localVue.use(Vuex);

const buildingFixture = require('./mockData/test_building_data.json');

describe('ResourcesModal', () => {
    let store;
    let wrapper;
    let getters;

    const levelUpStub = {
        name: 'LevelUpBuilding',
        template: '<button class="level-up-stub" @click="$emit(\'close\')"></button>',
    };

    beforeEach(() => {
        getters = {
            building: () => () => buildingFixture,
        };
        store = new Vuex.Store({ getters });
        wrapper = shallowMount(ResourcesModal, {
            localVue,
            store,
            propsData: { buildingId: 1 },
            stubs: {
                'level-up-building': levelUpStub,
            },
        });
    });

    afterEach(() => {
        wrapper.destroy();
    });

    it('renders resource stats so players see what the building produces', () => {
        // Assert: the modal should surface the building name, level, and production per hour.
        expect(wrapper.find('#buildingName').text()).toContain('Mine');
        expect(wrapper.find('#buildingDescription').text()).toContain('80 Stone');
    });

    it('re-emits close when LevelUpBuilding finishes so the modal can exit cleanly', async () => {
        // Act: simulate the child component notifying the parent that it wants to close.
        await wrapper.find('.level-up-stub').trigger('click');

        // Assert: ResourcesModal should bubble the event to any consumers.
        expect(wrapper.emitted('close')).toBeTruthy();
    });
});
