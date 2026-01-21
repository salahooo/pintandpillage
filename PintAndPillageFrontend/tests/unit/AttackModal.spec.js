
import { shallowMount } from '@vue/test-utils';
import { MockAttackModal } from './mockData/AttackModal.mock.js';

describe('AttackModal.vue', () => {
  let wrapper;

  const mountComponent = (propsData) => {
    wrapper = shallowMount(MockAttackModal, {
      propsData: {
        maxShips: 10,
        maxVikings: 50,
        ...propsData,
      },
    });
  };

  it('increments and decrements ship count within boundaries', async () => {
    mountComponent();
    
    // Initial count should be 0
    expect(wrapper.find('.ships-count').text()).toBe('0');

    // Clicking increment should increase the count
    await wrapper.find('[data-testid="increment-ships"]').trigger('click');
    expect(wrapper.find('.ships-count').text()).toBe('1');
    
    // Clicking decrement should decrease the count
    await wrapper.find('[data-testid="decrement-ships"]').trigger('click');
    expect(wrapper.find('.ships-count').text()).toBe('0');
  });

  it('does not allow ship count to go below zero', async () => {
    mountComponent();
    await wrapper.setData({ selectedShips: 0 });

    await wrapper.find('[data-testid="decrement-ships"]').trigger('click');
    expect(wrapper.find('.ships-count').text()).toBe('0');
  });

  it('does not allow ship count to exceed the maximum', async () => {
    mountComponent({ maxShips: 10 });
    await wrapper.setData({ selectedShips: 10 });

    await wrapper.find('[data-testid="increment-ships"]').trigger('click');
    expect(wrapper.find('.ships-count').text()).toBe('10');
  });

  it('increments and decrements viking count within boundaries', async () => {
    mountComponent();

    // Initial count should be 0
    expect(wrapper.find('.vikings-count').text()).toBe('0');

    // Clicking increment should increase the count
    await wrapper.find('[data-testid="increment-vikings"]').trigger('click');
    expect(wrapper.find('.vikings-count').text()).toBe('1');

    // Clicking decrement should decrease the count
    await wrapper.find('[data-testid="decrement-vikings"]').trigger('click');
    expect(wrapper.find('.vikings-count').text()).toBe('0');
  });

  it('does not allow viking count to go below zero', async () => {
    mountComponent();
    await wrapper.setData({ selectedVikings: 0 });

    await wrapper.find('[data-testid="decrement-vikings"]').trigger('click');
    expect(wrapper.find('.vikings-count').text()).toBe('0');
  });

  it('does not allow viking count to exceed the maximum', async () => {
    mountComponent({ maxVikings: 50 });
    await wrapper.setData({ selectedVikings: 50 });

    await wrapper.find('[data-testid="increment-vikings"]').trigger('click');
    expect(wrapper.find('.vikings-count').text()).toBe('50');
  });
});
