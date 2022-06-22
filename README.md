# GeoAedesAegypti
Modelo Aedes Aegypti basado en agentes realizado en Repast Simphony

<img src="https://github.com/Repastero/GeoAedesAegypti/blob/master/data/display.jpg" alt="Mapa" width="500"/>

## Optimizaciones
Para reducir los tiempos de simulación los **HumanAgent** se trasladan en orden de creación:
```
Ver "Programar movimiento de Humanos" en clase ContextCreator.
```
Para reducir los tiempos de simulación se puede aumentar el intervalo en que se trasladan los **HumanAgent**:
```
Ver método "switchHumanLocation" en clase ContextCreator.
```
## Display
Para visualizar los **BuildingAgent** en tiempo de simulación (causa uso intensivo de CPU y GPU):
```
Reemplazar el xml de display por: "repast.simphony.action.display_backup.xml" en GeoAedesAegypti.rs/
```
## Notas
Ver **TODOS** en código para mejoras o nuevas implementaciones que se pueden realizar.
