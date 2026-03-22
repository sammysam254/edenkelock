'use client';

interface Device {
  id: string;
  device_id: string;
  customer_id: string;
  status: string;
  total_amount: number;
  amount_paid: number;
  created_at: string;
}

export default function DeviceCard({ device }: { device: Device }) {
  const progress = (device.amount_paid / device.total_amount) * 100;

  return (
    <div className="bg-white rounded-2xl shadow-lg p-6 hover:shadow-2xl transition-all duration-300 border border-emerald-100 hover:border-emerald-300 transform hover:-translate-y-1">
      <div className="flex justify-between items-start mb-4">
        <div>
          <h3 className="text-xl font-bold text-gray-800">{device.device_id}</h3>
          <p className="text-sm text-gray-500 mt-1">Customer: {device.customer_id}</p>
        </div>
        <span className={`px-4 py-1.5 rounded-full text-xs font-semibold ${
          device.status === 'active' 
            ? 'bg-gradient-to-r from-green-100 to-emerald-100 text-green-700' 
            : 'bg-gradient-to-r from-red-100 to-pink-100 text-red-700'
        }`}>
          {device.status.toUpperCase()}
        </span>
      </div>

      <div className="mb-6">
        <div className="flex justify-between text-sm mb-2">
          <span className="text-gray-600 font-medium">Payment Progress</span>
          <span className="font-bold text-emerald-600">{progress.toFixed(0)}%</span>
        </div>
        <div className="w-full bg-gray-200 rounded-full h-3 overflow-hidden">
          <div 
            className="bg-gradient-to-r from-emerald-500 via-teal-500 to-green-500 h-3 rounded-full transition-all duration-500 shadow-sm"
            style={{ width: `${progress}%` }}
          ></div>
        </div>
      </div>

      <div className="flex justify-between text-sm mb-6 bg-gradient-to-r from-emerald-50 to-teal-50 rounded-lg p-4">
        <div>
          <p className="text-gray-600 text-xs mb-1">Amount Paid</p>
          <p className="font-bold text-lg text-emerald-600">${device.amount_paid.toFixed(2)}</p>
        </div>
        <div className="text-right">
          <p className="text-gray-600 text-xs mb-1">Total Amount</p>
          <p className="font-bold text-lg text-gray-800">${device.total_amount.toFixed(2)}</p>
        </div>
      </div>

      <button className="w-full bg-gradient-to-r from-emerald-500 to-teal-600 text-white py-3 rounded-xl font-semibold hover:from-emerald-600 hover:to-teal-700 transition-all duration-300 shadow-md hover:shadow-lg transform hover:scale-105">
        Manage Device
      </button>
    </div>
  );
}
