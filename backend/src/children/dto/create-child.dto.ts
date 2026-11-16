import { IsNotEmpty, IsString, IsDateString, IsOptional, IsUrl } from 'class-validator';
import { ApiProperty } from '@nestjs/swagger';

export class CreateChildDto {
  @ApiProperty({ example: 'Alex Johnson', description: 'Name or nickname of the child' })
  @IsString()
  @IsNotEmpty()
  name: string;

  @ApiProperty({ example: '2014-06-15', description: 'Birth date (YYYY-MM-DD) for age-appropriate presets' })
  @IsDateString()
  birthDate: string;

  @ApiProperty({ example: 'https://images.unsplash.com/avatar-child.png', required: false })
  @IsOptional()
  @IsUrl()
  avatarUrl?: string;
}
