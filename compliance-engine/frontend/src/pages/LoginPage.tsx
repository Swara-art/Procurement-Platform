import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { Shield, Lock, Mail, ArrowRight } from 'lucide-react';
import { useAuth } from '../context/AuthContext';
import { ActorType } from '../api/authApi';

export const LoginPage: React.FC = () => {
  const { login, register } = useAuth(); const navigate=useNavigate();
  const [registering,setRegistering]=useState(false); const [fullName,setFullName]=useState('');const [orgName,setOrgName]=useState('');const [regNumber,setRegNumber]=useState('');const [actorType,setActorType]=useState<ActorType>('BIDDER');
  const [email,setEmail]=useState('');const [password,setPassword]=useState('');const [error,setError]=useState('');const [notice,setNotice]=useState('');const [busy,setBusy]=useState(false);
  const showError=(e:unknown)=>{const err=e as {response?:{data?:{message?:string}}};setError(err.response?.data?.message||'The request could not be completed. Check your details and try again.');};
  const needsOrganization=actorType!=='ADMIN';
  const handleSubmit=async(e:React.FormEvent)=>{e.preventDefault();setBusy(true);setError('');setNotice('');try{if(registering){const result=await register({fullName,email,password,actorType,...(needsOrganization?{organizationName:orgName,registrationNumber:regNumber}:{})});if(result.status==='ACTIVE')navigate('/dashboard',{replace:true});else setNotice(result.message);}else{await login(email,password);navigate('/dashboard',{replace:true});}}catch(err){showError(err);}finally{setBusy(false);}};
  return <div className="min-h-screen bg-slate-50 flex flex-col justify-center py-12 sm:px-6 lg:px-8 font-sans">
    <div className="sm:mx-auto sm:w-full sm:max-w-md"><div className="flex justify-center"><div className="w-12 h-12 rounded-xl bg-navy-900 text-white flex items-center justify-center shadow-md"><Shield className="w-6 h-6"/></div></div><h2 className="mt-4 text-center text-xl font-bold tracking-tight text-slate-900">Government Tender & Bid Evaluation Portal</h2><p className="mt-1 text-center text-xs text-slate-500">Secure access for procurement authorities and registered bidders</p></div>
    <div className="mt-8 sm:mx-auto sm:w-full sm:max-w-md"><div className="bg-white py-8 px-6 sm:px-10 shadow-card rounded-xl border border-slate-200">
      <h3 className="text-sm font-semibold text-slate-800 mb-5">{registering?'Create an account or request access':'Sign in to your account'}</h3>
      {error&&<div className="mb-4 p-3 rounded-lg bg-rose-50 border border-rose-200 text-rose-700 text-xs">{error}</div>}
      {notice&&<div className="mb-4 p-3 rounded-lg bg-emerald-50 border border-emerald-200 text-emerald-800 text-xs">{notice}</div>}
      <form onSubmit={handleSubmit} className="space-y-4 text-xs">
        {registering&&<><label className="block font-semibold text-slate-700">I am registering as<select className="mt-1 w-full p-2 border border-slate-300 rounded-lg bg-white" value={actorType} onChange={e=>setActorType(e.target.value as ActorType)}><option value="BIDDER">Bidder</option><option value="ORGANIZATION">Government organization</option><option value="ADMIN">Admin</option></select></label><label className="block font-semibold text-slate-700">Full name<input className="mt-1 w-full p-2 border border-slate-300 rounded-lg" value={fullName} onChange={e=>setFullName(e.target.value)} autoComplete="name" required/></label>{needsOrganization&&<><label className="block font-semibold text-slate-700">{actorType==='BIDDER'?'Bidder organization':'Government organization / authority'}<input className="mt-1 w-full p-2 border border-slate-300 rounded-lg" value={orgName} onChange={e=>setOrgName(e.target.value)} required/></label><label className="block font-semibold text-slate-700">{actorType==='BIDDER'?'Business':'Government'} registration number<input className="mt-1 w-full p-2 border border-slate-300 rounded-lg" value={regNumber} onChange={e=>setRegNumber(e.target.value)} required/></label></>}</>}
        <label className="block font-semibold text-slate-700">Email<div className="relative mt-1"><Mail className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input type="email" className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg" value={email} onChange={e=>setEmail(e.target.value)} autoComplete="email" required/></div></label>
        <label className="block font-semibold text-slate-700">Password<div className="relative mt-1"><Lock className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-400"/><input type="password" minLength={registering?12:1} className="w-full pl-9 pr-3 py-2 border border-slate-300 rounded-lg" value={password} onChange={e=>setPassword(e.target.value)} autoComplete={registering?'new-password':'current-password'} required/></div>{registering&&<span className="text-[10px] text-slate-500">Use at least 12 characters.</span>}</label>
        <button disabled={busy} className="w-full mt-2 flex items-center justify-center gap-2 py-2.5 px-4 rounded-lg shadow-sm text-xs font-semibold text-white bg-navy-900 hover:bg-navy-800 disabled:opacity-50"><span>{busy?'Please wait…':registering?(actorType==='BIDDER'?'Create bidder account':'Request access'):'Sign in'}</span><ArrowRight className="w-4 h-4"/></button>
      </form>
      <button onClick={()=>{setRegistering(!registering);setError('');setNotice('');}} className="mt-5 w-full text-center text-xs font-medium text-navy-900 hover:underline">{registering?'Already registered? Sign in':'Create account / request access'}</button>
      <p className="mt-5 pt-4 border-t border-slate-100 text-center text-[10px] text-slate-500">Bidder accounts activate immediately. Government organization and admin access requests require system administrator approval.</p>
    </div><p className="mt-4 text-center text-[11px] text-slate-400">Decision support system. Final contract approval remains under authorized human control.</p></div>
  </div>;
};
