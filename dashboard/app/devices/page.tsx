'use client'

import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'
import DeviceCard from '@/components/DeviceCard'
import { Smartphone, Plus, Search, Filter } from 'lucide-react'

export default function DevicesPage() {
  const [devices, setDevices] = useState<any[]>([])
  const [loading, setLoading] = useState(true)
  const [searchTerm, setSearchTerm] = useState('')

  useEffect(() => {
    loadDevices()
  }, [])

  async function loadDevices() {
    const { data, error } = await supabase
      .from('devices')
      .select('*, customers(*)')
      .order('created_at', { ascending: false })

    if (!error && data) {
      setDevices(data)
    }
    setLoading(false)
  }

  const filteredDevices = devices.filter(device =>
    device.device_code.toLowerCase().includes(searchTerm.toLowerCase()) ||
    device.device_model.toLowerCase().includes(searchTerm.toLowerCase()) ||
    device.customers?.full_name.toLowerCase().includes(searchTerm.toLowerCase())
  )

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 via-blue-50 to-purple-50 flex items-center justify-center">
        <div className="text-center">
          <Smartphone className="w-12 h-12 animate-pulse text-primary mx-auto mb-4" />
          <p className="text-gray-600">Loading devices...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-blue-50 to-purple-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-6">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-3">
                <Smartphone className="w-8 h-8 text-primary" />
                Devices
              </h1>
              <p className="text-gray-600 mt-1">{devices.length} total devices</p>
            </div>
            <button className="bg-gradient-to-r from-green-500 to-green-600 text-white px-6 py-3 rounded-lg hover:shadow-lg transition-all duration-200 flex items-center gap-2">
              <Plus className="w-5 h-5" />
              Enroll Device
            </button>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Search and Filter */}
        <div className="bg-white rounded-xl shadow-lg p-4 mb-6 flex items-center gap-4">
          <div className="flex-1 relative">
            <Search className="w-5 h-5 absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400" />
            <input
              type="text"
              placeholder="Search by device code, model, or customer..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-primary focus:border-transparent"
            />
          </div>
          <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 flex items-center gap-2">
            <Filter className="w-5 h-5" />
            Filter
          </button>
        </div>

        {/* Devices Grid */}
        {filteredDevices.length === 0 ? (
          <div className="bg-white rounded-xl shadow-lg p-12 text-center">
            <Smartphone className="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <h3 className="text-xl font-semibold text-gray-900 mb-2">No devices found</h3>
            <p className="text-gray-600 mb-6">Get started by enrolling your first device</p>
            <button className="bg-gradient-to-r from-green-500 to-green-600 text-white px-6 py-3 rounded-lg hover:shadow-lg transition-all duration-200">
              Enroll First Device
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
            {filteredDevices.map(device => (
              <DeviceCard key={device.id} device={device} />
            ))}
          </div>
        )}
      </div>
    </div>
  )
}
