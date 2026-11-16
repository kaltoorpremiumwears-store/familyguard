import { IsEmail, IsNotEmpty, IsString, MinLength, IsBoolean } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class RegisterDto {
  @ApiProperty({ example: 'guardian@family.org', description: 'Parent or Guardian Email Address' })
  @IsEmail()
  email: string;

  @ApiProperty({ example: 'SecurePassword123!', description: 'Strong password (minimum 8 characters)' })
  @IsString()
  @MinLength(8)
  password: string;

  @ApiProperty({ example: 'Sarah Johnson', description: 'Parent Full Name' })
  @IsString()
  @IsNotEmpty()
  fullName: string;

  @ApiProperty({ example: '+15551234567', required: false, description: 'Optional emergency contact number' })
  phoneNumber?: string;

  @ApiProperty({
    example: true,
    description: 'Verifiable Parental Consent (VPC) - Confirmation of legal authority over child profiles',
  })
  @IsBoolean()
  hasConsentedToParentalTerms: boolean;
}
