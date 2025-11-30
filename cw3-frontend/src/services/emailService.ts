import emailjs from '@emailjs/browser';

// EmailJS Configuration
const EMAILJS_SERVICE_ID = 'service_4m4syta';
const EMAILJS_TEMPLATE_ID = 'template_ngcdmss';
const EMAILJS_PUBLIC_KEY = 'cWGoQtbtaC7gb40jz';

// Initialize EmailJS
emailjs.init(EMAILJS_PUBLIC_KEY);

export interface FlightStats {
  totalMoves: number;
  totalCost: number;
  numDrones: number;
  droneIds: number[];
  deliveryIds: number[];
}

export interface EmailData {
  toEmail: string;
  stats: FlightStats;
  recommendationNote?: string;
}

/**
 * Send flight stats email via EmailJS
 */
export const sendFlightStatsEmail = async (emailData: EmailData): Promise<{ success: boolean; message: string }> => {
  try {
    const timestamp = new Date().toLocaleString('en-GB', {
      dateStyle: 'full',
      timeStyle: 'short',
    });

    const templateParams = {
      to_email: emailData.toEmail,
      total_moves: emailData.stats.totalMoves.toString(),
      total_cost: emailData.stats.totalCost.toFixed(2),
      num_drones: emailData.stats.numDrones.toString(),
      drone_ids: emailData.stats.droneIds.join(', ') || 'N/A',
      delivery_ids: emailData.stats.deliveryIds.join(', ') || 'N/A',
      timestamp: timestamp,
      recommendation: emailData.recommendationNote || '',
    };

    console.log('Sending email with params:', templateParams);

    const response = await emailjs.send(
      EMAILJS_SERVICE_ID,
      EMAILJS_TEMPLATE_ID,
      templateParams
    );

    console.log('EmailJS response:', response);

    if (response.status === 200) {
      return { success: true, message: 'Email sent successfully!' };
    } else {
      return { success: false, message: `Failed to send email: status ${response.status}` };
    }
  } catch (error: any) {
    console.error('EmailJS error:', error);
    // Extract more detailed error message
    const errorMessage = error?.text || error?.message || 'Failed to send email';
    return { 
      success: false, 
      message: errorMessage
    };
  }
};

/**
 * Validate email format
 */
export const isValidEmail = (email: string): boolean => {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
};

