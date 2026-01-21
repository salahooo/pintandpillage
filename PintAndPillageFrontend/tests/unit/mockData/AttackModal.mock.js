// This is a mock component for AttackModal.vue for testing purposes.
export const MockAttackModal = {
  name: 'AttackModal',
  props: {
    maxShips: { type: Number, default: 10 },
    maxVikings: { type: Number, default: 50 },
  },
  data() {
    return {
      selectedShips: 0,
      selectedVikings: 0,
    };
  },
  template: `
    <div>
      <span class="ships-count">{{ selectedShips }}</span>
      <button @click="decrementShips" data-testid="decrement-ships">-</button>
      <button @click="incrementShips" data-testid="increment-ships">+</button>

      <span class="vikings-count">{{ selectedVikings }}</span>
      <button @click="decrementVikings" data-testid="decrement-vikings">-</button>
      <button @click="incrementVikings" data-testid="increment-vikings">+</button>
    </div>
  `,
  methods: {
    incrementShips() {
      if (this.selectedShips < this.maxShips) this.selectedShips++;
    },
    decrementShips() {
      if (this.selectedShips > 0) this.selectedShips--;
    },
    incrementVikings() {
      if (this.selectedVikings < this.maxVikings) this.selectedVikings++;
    },
    decrementVikings() {
      if (this.selectedVikings > 0) this.selectedVikings--;
    },
  },
};
