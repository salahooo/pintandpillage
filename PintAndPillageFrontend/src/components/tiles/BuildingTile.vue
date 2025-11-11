<template>
    <div class="clickableTile" :data-testid="tileTestId">
        <img class="tileImg" :src="getTileSource()"/>
    </div>
</template>

<script>
    /* eslint-disable no-console */

    export default{
        props: ['buildingProperties'],

       computed:{
            seasonsOn: function () {
                return this.$store.state.seasonsEnabled
            },
            currentSeason: function () {
                return this.$store.state.currentSeason
            },
            tileTestId: function () {
                if (!this.buildingProperties || !this.buildingProperties.position) {
                    return null;
                }
                const { x, y } = this.buildingProperties.position;
                return `build-tile-${x}-${y}`; // REFACTOR (ITSTEN H2): Add stable selector so E2E tests can target build tiles.
            }
        },
        methods:{
            getTileSource: function () {
                if (this.seasonsOn && this.currentSeason === 'winter'){
                    return require('../../assets/winterTiles/building_spot.png')
                }else{
                    return require('../../assets/tiles/building_spot.png')
                }
            }
        }
    }
</script>

<style>

</style>
