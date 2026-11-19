import { icon, type LeafletEventHandlerFnMap, type Marker as LeafletMarker } from 'leaflet'
import markerIcon2xUrl from 'leaflet/dist/images/marker-icon-2x.png'
import markerIconUrl from 'leaflet/dist/images/marker-icon.png'
import markerShadowUrl from 'leaflet/dist/images/marker-shadow.png'
import 'leaflet/dist/leaflet.css'
import { useEffect, useMemo } from 'react'
import { MapContainer, Marker, TileLayer, useMap, useMapEvents } from 'react-leaflet'
import type { LatLon } from '../api/attractions.ts'

/** Sinaia, where the map starts when there is no pin yet. */
const DEFAULT_CENTER: [number, number] = [45.35, 25.55]
const DEFAULT_ZOOM = 13
const PIN_ZOOM = 15

const OSM_TILES = 'https://tile.openstreetmap.org/{z}/{x}/{y}.png'
const OSM_ATTRIBUTION =
  '&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'

// Leaflet looks for its marker images next to its CSS, which a bundler moves.
const pinIcon = icon({
  iconUrl: markerIconUrl,
  iconRetinaUrl: markerIcon2xUrl,
  shadowUrl: markerShadowUrl,
  iconSize: [25, 41],
  iconAnchor: [12, 41],
  shadowSize: [41, 41],
})

export interface LocationMapProps {
  position: LatLon | null
  onChange: (position: LatLon) => void
  disabled?: boolean
}

function ClickToPlace({ onChange }: Pick<LocationMapProps, 'onChange'>) {
  useMapEvents({
    click(event) {
      const { lat, lng } = event.latlng.wrap()
      onChange({ lat, lon: lng })
    },
  })
  return null
}

/** Brings a pin typed into the coordinate fields into view. */
function KeepInView({ position }: { position: LatLon }) {
  const map = useMap()
  useEffect(() => {
    const target: [number, number] = [position.lat, position.lon]
    if (!map.getBounds().contains(target)) map.panTo(target)
  }, [map, position.lat, position.lon])
  return null
}

/** OpenStreetMap map to place the attraction's pin, by clicking or dragging. */
export default function LocationMap({ position, onChange, disabled = false }: LocationMapProps) {
  const eventHandlers = useMemo<LeafletEventHandlerFnMap>(
    () => ({
      dragend(event) {
        const { lat, lng } = (event.target as LeafletMarker).getLatLng().wrap()
        onChange({ lat, lon: lng })
      },
    }),
    [onChange],
  )

  return (
    <MapContainer
      className="location-map"
      center={position ? [position.lat, position.lon] : DEFAULT_CENTER}
      zoom={position ? PIN_ZOOM : DEFAULT_ZOOM}
      scrollWheelZoom={false}
    >
      <TileLayer url={OSM_TILES} attribution={OSM_ATTRIBUTION} maxZoom={19} />
      {!disabled && <ClickToPlace onChange={onChange} />}
      {position && (
        <>
          <Marker
            position={[position.lat, position.lon]}
            icon={pinIcon}
            draggable={!disabled}
            eventHandlers={eventHandlers}
          />
          <KeepInView position={position} />
        </>
      )}
    </MapContainer>
  )
}
