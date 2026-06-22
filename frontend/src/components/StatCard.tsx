interface StatCardProps {
  label: string;
  value: number;
  sub: string;
  color: 'blue' | 'green' | 'orange' | 'red';
}

const colorMap: Record<StatCardProps['color'], string> = {
  blue:   'text-blue-600',
  green:  'text-green-600',
  orange: 'text-orange-500',
  red:    'text-red-500',
};

export default function StatCard({ label, value, sub, color }: StatCardProps) {
  return (
    <div className="bg-white rounded-xl shadow-sm p-5 border border-gray-100">
      <p className="text-xs text-gray-400 mb-1">{sub}</p>
      <p className={`text-3xl font-bold ${colorMap[color]}`}>{value}</p>
      <p className="text-sm text-gray-600 mt-1">{label}</p>
    </div>
  );
}
