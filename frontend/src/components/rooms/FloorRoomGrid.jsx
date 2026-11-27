import RoomCard from './RoomCard';

export default function FloorRoomGrid({ rooms }) {
  const sorted = [...(rooms || [])].sort((a, b) => a.roomNumber - b.roomNumber);

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
      {sorted.map((room) => (
        <RoomCard key={room.roomNumber} room={room} />
      ))}
    </div>
  );
}
