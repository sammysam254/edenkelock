'use client'

import { useEffect, useState } from 'react'
import { supabase } from '@/lib/supabase'
import { BarChart3, Users, Smartphone, DollarSign, TrendingUp, Lock, Unlock, Activity } from 'lucide-react'
import Link from 'next/link'

export default function Dashboard() {
  const [stats, setStats] = useState({
    totalDevices: 0,
    activeDevices: 0,
    totalCustomers: 0,
    totalRevenue: 0,
    lockedDevices: 0,
    unlockedDevices: 0,
  })
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    loadStats()
  }, [])

  async function loadStats() {
    try {
      const [devices, customers, payments] = await Promise.all([
        supabase.from('devices').select('*', { count: 'exact' }),
        supabase.from('customers').select('*', { count: 'exact' }),
        supabase.from('payment_transactions').select('amount'),
      ])

      const activeDevices = devices.data?.filter(d => d.status === 'active').length || 0
      const lockedDevices = devices.data?.filter(d => d.is_locked).length || 0
      const unlockedDevices = devices.data?.filter(d => !d.is_locked).length || 0
      const totalRevenue = payments.data?.reduce((sum, p) => sum + parseFloat(p.amount), 0) || 0

      setStats({
        totalDevices: devices.count || 0,
        activeDevices,
        totalCustomers: customers.count || 0,
        totalRevenue,
        lockedDevices,
        unlockedDevices,
      })
    } catch (error) {
      console.error('Error loading stats:', error)
    } finally {
      setLoading(false)
    }
  }

  if (loading) {
    return (
      <div className="min-h-screen bg-gradient-to-br from-green-50 via-blue-50 to-purple-50 flex items-center justify-center">
        <div className="text-center">
          <Activity className="w-12 h-12 animate-spin text-primary mx-auto mb-4" />
          <p className="text-gray-600">Loading dashboard...</p>
        </div>
      </div>
    )
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-green-50 via-blue-50 to-purple-50">
      {/* Header */}
      <div className="bg-white shadow-sm border-b">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <div className="bg-gradient-to-br from-green-500 to-green-600 p-2 rounded-lg">
                <Smartphone className="w-6 h-6 text-white" />
              </div>
              <div>
                <h1 className="text-2xl font-bold text-gray-900">Eden M-Kopa</h1>
                <p className="text-sm text-gray-600">Device Financing Platform</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className="w-2 h-2 bg-green-500 rounded-full animate-pulse"></div>
              <span className="text-sm text-gray-600">System Online</span>
            </div>
          </div>
        </div>
      </div>

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
        {/* Welcome Banner */}
        <div className="bg-gradient-to-r from-green-600 to-blue-600 rounded-2xl shadow-xl p-8 mb-8 text-white">
          <h2 className="text-3xl font-bold mb-2">Welcome Back! 👋</h2>
          <p className="text-green-100">Manage your device financing operations from one place</p>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
          <StatCard
            icon={<Smartphone className="w-8 h-8" />}
            title="Total Devices"
            value={stats.totalDevices}
            color="from-blue-500 to-blue-600"
            trend="+12%"
          />
          <StatCard
            icon={<Activity className="w-8 h-8" />}
            title="Active Devices"
            value={stats.activeDevices}
            color="from-green-500 to-green-600"
            trend="+8%"
          />
          <StatCard
            icon={<Users className="w-8 h-8" />}
            title="Total Customers"
            value={stats.totalCustomers}
            color="from-purple-500 to-purple-600"
            trend="+15%"
          />
          <StatCard
            icon={<DollarSign className="w-8 h-8" />}
            title="Total Revenue"
            value={`KES ${stats.totalRevenue.toLocaleString()}`}
            color="from-orange-500 to-orange-600"
            trend="+23%"
          />
        </div>

        {/* Lock Status */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 mb-8">
          <div className="bg-white rounded-xl shadow-lg p-6 border border-red-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Locked Devices</h3>
              <Lock className="w-6 h-6 text-red-500" />
            </div>
            <p className="text-4xl font-bold text-red-600">{stats.lockedDevices}</p>
            <p className="text-sm text-gray-600 mt-2">Awaiting payment</p>
          </div>
          <div className="bg-white rounded-xl shadow-lg p-6 border border-green-100">
            <div className="flex items-center justify-between mb-4">
              <h3 className="text-lg font-semibold text-gray-900">Unlocked Devices</h3>
              <Unlock className="w-6 h-6 text-green-500" />
            </div>
            <p className="text-4xl font-bold text-green-600">{stats.unlockedDevices}</p>
            <p className="text-sm text-gray-600 mt-2">Payment up to date</p>
          </div>
        </div>

        {/* Quick Actions */}
        <div className="bg-white rounded-xl shadow-lg p-6 mb-8">
          <h2 className="text-xl font-semibold mb-6 flex items-center gap-2">
            <TrendingUp className="w-6 h-6 text-primary" />
            Quick Actions
          </h2>
          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            <Link href="/devices">
              <button className="w-full p-6 border-2 border-green-500 rounded-xl hover:bg-green-500 hover:text-white transition-all duration-200 group">
                <Smartphone className="w-8 h-8 mx-auto mb-2 text-green-500 group-hover:text-white" />
                <p className="font-semibold">Enroll New Device</p>
                <p className="text-sm text-gray-600 group-hover:text-green-100 mt-1">Add device to system</p>
              </button>
            </Link>
            <button className="w-full p-6 border-2 border-blue-500 rounded-xl hover:bg-blue-500 hover:text-white transition-all duration-200 group">
              <DollarSign className="w-8 h-8 mx-auto mb-2 text-blue-500 group-hover:text-white" />
              <p className="font-semibold">Process Payment</p>
              <p className="text-sm text-gray-600 group-hover:text-blue-100 mt-1">Record customer payment</p>
            </button>
            <button className="w-full p-6 border-2 border-purple-500 rounded-xl hover:bg-purple-500 hover:text-white transition-all duration-200 group">
              <BarChart3 className="w-8 h-8 mx-auto mb-2 text-purple-500 group-hover:text-white" />
              <p className="font-semibold">View Reports</p>
              <p className="text-sm text-gray-600 group-hover:text-purple-100 mt-1">Analytics & insights</p>
            </button>
          </div>
        </div>

        {/* Footer */}
        <div className="text-center text-gray-600 text-sm">
          <p>© 2024 Eden M-Kopa. Powered by Supabase & Render.</p>
        </div>
      </div>
    </div>
  )
}

function StatCard({ icon, title, value, color, trend }: any) {
  return (
    <div className="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-shadow duration-200">
      <div className={`bg-gradient-to-br ${color} text-white w-14 h-14 rounded-xl flex items-center justify-center mb-4 shadow-lg`}>
        {icon}
      </div>
      <h3 className="text-gray-600 text-sm mb-1">{title}</h3>
      <div className="flex items-end justify-between">
        <p className="text-3xl font-bold text-gray-900">{value}</p>
        <span className="text-green-600 text-sm font-semibold flex items-center gap-1">
          <TrendingUp className="w-4 h-4" />
          {trend}
        </span>
      </div>
    </div>
  )
}
