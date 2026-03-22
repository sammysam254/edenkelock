'use client'

import { Smartphone, Lock, Unlock, Calendar, DollarSign } from 'lucide-react'

interface Device {
  id: string
  device_code: string
  device_model: string
  loan_balance: number
  total_paid: number
  is_locked: boolean
  status: string
  next_payment_due: string
  customers?: { full_name: string }
}

export default function DeviceCard({ device }: { device: Device }) {
  const progress = device.total_paid / (device.loan_balance + device.total_paid) * 100

  return (
    <div className="bg-white rounded-xl shadow-lg hover:shadow-2xl transition-all duration-300 overflow-hidden border border-gray-100">
      {/* Status Banner */}
      <div className={`h-2 ${device.is_locked ? 'bg-gradient-to-r from-red-500 to-red-600' : 'bg-gradient-to-r from-green-500 to-green-600'}`}></div>
      
      <div className="p-6">
        {/* Header */}
        <div className="flex items-start justify-between mb-4">
          <div className="flex items-center gap-3">
            <div className={`p-3 rounded-xl ${device.is_locked ? 'bg-red-100' : 'bg-green-100'}`}>
              <Smartphone className={`w-6 h-6 ${device.is_locked ? 'text-red-600' : 'text-green-600'}`} />
            </div>
            <div>
              <h3 className="font-bold text-lg text-gray-900">{device.device_code}</h3>
              <p className="text-sm text-gray-600">{device.device_model}</p>
            </div>
          </div>
          <div className={`p-2 rounded-lg ${device.is_locked ? 'bg-red-100' : 'bg-green-100'}`}>
            {device.is_locked ? (
              <Lock className="w-5 h-5 text-red-600" />
            ) : (
              <Unlock className="w-5 h-5 text-green-600" />
            )}
          </div>
        </div>

        {/* Customer */}
        <div className="mb-4 p-3 bg-gray-50 rounded-lg">
          <p className="text-xs text-gray-600 mb-1">Customer</p>
          <p className="font-semibold text-gray-900">{device.customers?.full_name || 'N/A'}</p>
        </div>

        {/* Progress Bar */}
        <div className="mb-4">
          <div className="flex justify-between text-xs text-gray-600 mb-2">
            <span>Payment Progress</span>
            <span>{progress.toFixed(0)}%</span>
          </div>
          <div className="w-full bg-gray-200 rounded-full h-2">
            <div 
              className="bg-gradient-to-r from-green-500 to-green-600 h-2 rounded-full transition-all duration-500"
              style={{ width: `${progress}%` }}
            ></div>
          </div>
        </div>

        {/* Details */}
        <div className="space-y-3">
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600 flex items-center gap-2">
              <DollarSign className="w-4 h-4" />
              Balance
            </span>
            <span className="font-bold text-gray-900">KES {device.loan_balance.toLocaleString()}</span>
          </div>
          <div className="flex items-center justify-between text-sm">
            <span className="text-gray-600 flex items-center gap-2">
              <DollarSign className="w-4 h-4" />
              Paid
            </span>
            <span className="font-semibold text-green-600">KES {device.total_paid.toLocaleString()}</span>
          </div>
          {device.next_payment_due && (
            <div className="flex items-center justify-between text-sm">
              <span className="text-gray-600 flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                Next Due
              </span>
              <span className="font-medium text-gray-900">
                {new Date(device.next_payment_due).toLocaleDateString()}
              </span>
            </div>
          )}
        </div>

        {/* Status Badge */}
        <div className="mt-4 pt-4 border-t border-gray-100">
          <span className={`inline-flex items-center px-3 py-1 rounded-full text-xs font-semibold ${
            device.status === 'active' ? 'bg-green-100 text-green-700' : 
            device.status === 'paid_off' ? 'bg-blue-100 text-blue-700' : 
            'bg-red-100 text-red-700'
          }`}>
            {device.status.toUpperCase().replace('_', ' ')}
          </span>
        </div>
      </div>
    </div>
  )
}
