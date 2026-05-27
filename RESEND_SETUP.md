# Resend Setup Guide

## 🚀 Get Your Resend API Key

Follow these steps to get your free Resend API key:

### Step 1: Sign Up
1. Go to: **[resend.com/signup](https://resend.com/signup)**
2. Sign up with your email
3. **No credit card required** ✅
4. Verify your email address

### Step 2: Create API Key
1. After logging in, go to **API Keys**
2. Click **Create API Key**
3. Settings:
   - **Name**: `Chat Platform Password Reset`
   - **Permission**: `Sending access`
4. Click **Create**
5. **Copy the API key** (starts with `re_`) - you'll need this!

### Step 3: Email Configuration

**For Testing (Quick Start)**:
- Use Resend's test email: `onboarding@resend.dev`
- This works immediately, no domain setup needed

**For Production (Optional)**:
- Go to **Domains** → **Add Domain**
- Add your custom domain (e.g., `yourdomain.com`)
- Follow DNS setup instructions
- Use email like: `noreply@yourdomain.com`

---

## ✅ What to Do Next

Once you have the API key, reply with:

**"I have the API key"** 

And paste the key (it will look like: `re_123abc...`)

I'll then:
1. Add it to your environment variables
2. Implement the complete password reset feature
3. Test it end-to-end

---

## 📊 Free Tier Limits

- **100 emails per day**
- **3,000 emails per month**
- Perfect for password resets!

---

## 🔒 Security Note

The API key is sensitive - I'll add it to your Render environment variables (encrypted), not to your code repository.
