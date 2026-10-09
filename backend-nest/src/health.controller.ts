import { Controller, Get } from '@nestjs/common';
import { Access } from './common/access';

@Controller('actuator/health')
@Access('public')
export class HealthController {
  @Get()
  health() {
    return { status: 'UP' };
  }
}
